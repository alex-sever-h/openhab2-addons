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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
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

    ScheduledFuture<?> refreshJob;

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

    private void startAutomaticRefresh() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
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
                        onoff = true;
                    } else {
                        onoff = false;
                    }

                    // JsonElement resp = new JsonParser().parse(jsonResponse);
                    // temperature = resp.getAsJsonObject().get("sensors").getAsJsonObject().get("temperature")
                    // .getAsFloat();
                    // humidity = resp.getAsJsonObject().get("sensors").getAsJsonObject().get("humidity").getAsInt();
                    //
                    // updateState(new ChannelUID(getThing().getUID(), CHANNEL_TEMPERATURE), getTemperature());
                    updateState(new ChannelUID(getThing().getUID(), CHANNEL_ONOFF), getOnOff());
                } catch (Exception e) {
                    logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
                    e.printStackTrace();
                }
            }
        };

        refreshJob = scheduler.scheduleAtFixedRate(runnable, 0, refresh.intValue(), TimeUnit.SECONDS);
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
