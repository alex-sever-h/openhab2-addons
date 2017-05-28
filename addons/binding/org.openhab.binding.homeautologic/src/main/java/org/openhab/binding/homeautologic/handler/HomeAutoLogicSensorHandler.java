/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.homeautologic.handler;

import static org.openhab.binding.homeautologic.HomeAutoLogicBindingConstants.*;

import java.math.BigDecimal;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.PercentType;
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
 * The {@link HomeAutoLogicSensorHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alexandru-Sever Horin - Initial contribution
 */
public class HomeAutoLogicSensorHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(HomeAutoLogicSensorHandler.class);

    private String ipAddress;
    private int port;
    private BigDecimal refresh;

    float temperature = 0;
    int humidity = 0;

    ScheduledFuture<?> refreshJob;

    public HomeAutoLogicSensorHandler(Thing thing) {
        super(thing);
    }

    private State getTemperature() {
        return new DecimalType(temperature);
    }

    private State getHumidity() {
        return new PercentType(humidity);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Note: if communication with thing fails for some reason,
        // indicate that by setting the status with detail information
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
        // "Could not control device at IP address x.x.x.x");
        switch (channelUID.getId()) {
            case CHANNEL_TEMPERATURE:
                updateState(channelUID, getTemperature());
                break;
            case CHANNEL_HUMIDITY:
                updateState(channelUID, getHumidity());
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
                    String address = "http://" + ipAddress + "/config?command=sensor";
                    System.out.println(address);

                    String data = HttpUtils.getStringData(address);

                    JsonObject jsonObject = new JsonParser().parse(data).getAsJsonObject();

                    JsonObject jDevice = jsonObject.get("response").getAsJsonObject();
                    JsonElement jProduct = jDevice.get("temperature");

                    float temp = jProduct.getAsInt();
                    temperature = temp / 100.0f;
                    // String jsonResponse = HttpUtils.getStringData(ipAddress, port);
                    // logger.debug(jsonResponse);
                    //
                    // JsonElement resp = new JsonParser().parse(jsonResponse);
                    // temperature = resp.getAsJsonObject().get("sensors").getAsJsonObject().get("temperature")
                    // .getAsFloat();
                    // humidity = resp.getAsJsonObject().get("sensors").getAsJsonObject().get("humidity").getAsInt();
                    //
                    updateState(new ChannelUID(getThing().getUID(), CHANNEL_TEMPERATURE), getTemperature());
                    // updateState(new ChannelUID(getThing().getUID(), CHANNEL_HUMIDITY), getHumidity());
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
