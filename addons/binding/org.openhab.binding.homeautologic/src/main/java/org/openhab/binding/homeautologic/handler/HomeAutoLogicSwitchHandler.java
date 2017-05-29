/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.homeautologic.handler;

import static org.openhab.binding.homeautologic.HomeAutoLogicBindingConstants.CHANNEL_ONOFF;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.no.rules.Predictor;
import org.openhab.binding.homeautologic.config.HomeAutoLogicConfig;
import org.openhab.binding.homeautologic.internal.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The {@link HomeAutoLogicSwitchHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alexandru-Sever Horin - Initial contribution
 */
public class HomeAutoLogicSwitchHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(HomeAutoLogicSwitchHandler.class);

    private String ipAddress;
    private int port;
    private BigDecimal refresh;

    boolean onoff = true;

    int windowSize = 5;
    int windowIndex = 0;
    String[] window = new String[windowSize];
    Predictor predictor = new Predictor();

    boolean predictorTrained = false;
    ScheduledFuture<?> refreshJob;

    ScheduledFuture<?> trainJob;

    static private ItemRegistry itemRegistry;

    public void setItemRegistry(ItemRegistry itemRegistry_) {
        itemRegistry = itemRegistry_;
    }

    public HomeAutoLogicSwitchHandler(Thing thing) {
        super(thing);
    }

    private State getOnOff() {
        OnOffType on_off;
        if (onoff) {
            on_off = OnOffType.ON;
        } else {
            on_off = OnOffType.OFF;
        }
        return on_off;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Note: if communication with thing fails for some reason,
        // indicate that by setting the status with detail information
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
        // "Could not control device at IP address x.x.x.x");
        switch (channelUID.getId()) {
            case CHANNEL_ONOFF:
                String cmd = null;
                if (command == OnOffType.ON) {
                    cmd = "1";
                } else {
                    cmd = "0";
                }
                String address = "http://" + ipAddress + "/config?command=switch";
                String postData = "{\"Response\":{\"status\":" + cmd + "}}";
                try {
                    HttpUtils.post(address, postData);
                } catch (Exception e) {
                    logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
                    e.printStackTrace();
                }
                updateState(channelUID, getOnOff());
                break;

            default:
                logger.debug("Command received for an unknown channel: {}", channelUID.getId());
                break;
        }
    }

    int swIndex = -1;

    private void startAutomaticRefresh() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    boolean lonoff;
                    String jsonResponse = HttpUtils.getStringData(ipAddress, port);
                    logger.debug(jsonResponse);

                    String address = "http://" + ipAddress + "/config?command=switch";
                    System.out.println(address);

                    String data = HttpUtils.getStringData(address);

                    JsonObject jsonObject = new JsonParser().parse(data).getAsJsonObject();

                    JsonObject jDevice = jsonObject.get("response").getAsJsonObject();
                    JsonElement jProduct = jDevice.get("status");

                    int status = jProduct.getAsInt();

                    if (status != 0) {
                        lonoff = true;
                    } else {
                        lonoff = false;
                    }

                    if (lonoff != onoff) {
                        // TODO: Save sample
                    }

                    onoff = lonoff;

                    // JsonElement resp = new JsonParser().parse(jsonResponse);
                    // temperature = resp.getAsJsonObject().get("sensors").getAsJsonObject().get("temperature")
                    // .getAsFloat();
                    // humidity = resp.getAsJsonObject().get("sensors").getAsJsonObject().get("humidity").getAsInt();
                    //
                    // updateState(new ChannelUID(getThing().getUID(), CHANNEL_TEMPERATURE), getTemperature());
                    updateState(new ChannelUID(getThing().getUID(), CHANNEL_ONOFF), getOnOff());

                    Collection<Item> items = itemRegistry.getItems();

                    StringBuffer sb = new StringBuffer();

                    Iterator<Item> iterator = items.iterator();

                    java.util.List<String> whitelist = new ArrayList<String>();

                    whitelist.add("Temperature");
                    whitelist.add("Switch");
                    whitelist.add("Humidity");

                    boolean first = true;

                    int counter = 0;

                    while (iterator.hasNext()) {

                        Item item = iterator.next();

                        String category = item.getCategory();

                        if (whitelist.contains(category)) {
                            if (category.equals("Switch")) {
                                swIndex = counter;
                            }

                            String value = item.getState().toFullString();

                            if (first) {
                                first = false;
                            } else {
                                sb.append(",");
                            }

                            sb.append(value);
                            counter++;

                        }
                    }

                    int lastIndex = windowIndex;

                    if (counter == 3) {
                        String newValue = sb.toString();

                        window[windowIndex++] = newValue;
                    }
                    if (windowIndex >= windowSize) {
                        windowIndex = 0;

                        try {
                            predictor.train(window, swIndex);
                            predictorTrained = true;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (predictorTrained) {
                        double[] prediction = predictor.predict(window[lastIndex], swIndex);

                        System.out.println("Prediction for last value: ");
                        for (int i = 0; i < prediction.length; i++) {
                            System.out.println("value " + i + " prediction: " + prediction[i]);
                        }
                    }

                    System.out.println("new value " + sb.toString());
                } catch (Exception e) {
                    logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
                    e.printStackTrace();
                }
            }
        };

        refreshJob = scheduler.scheduleAtFixedRate(runnable, 0, refresh.intValue(), TimeUnit.SECONDS);
    }

    private void startPeriodicTrain() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    // Thread.sleep(25000);
                    predictor.train(window, swIndex);
                    predictorTrained = true;

                } catch (Exception e) {
                    logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
                    e.printStackTrace();
                }
            }
        };

        // trainJob = scheduler.scheduleAtFixedRate(runnable, 0, 60, TimeUnit.SECONDS);
    }

    @Override
    public void initialize() {
        super.initialize();

        logger.debug("[alseh] My Home automation thingy");

        this.port = 80;

        HomeAutoLogicConfig config = getConfigAs(HomeAutoLogicConfig.class);
        this.ipAddress = config.ip;
        this.refresh = new BigDecimal(config.refresh);

        if (StringUtils.isEmpty(this.ipAddress)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR, "IP address is not set");
            return;
        }

        startAutomaticRefresh();

        updateStatus(ThingStatus.ONLINE);
    }
}
