/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.homeautologic.discovery;

import static org.openhab.binding.homeautologic.HomeAutoLogicBindingConstants.*;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.homeautologic.internal.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 *
 * The {@link HomeAutoLogicDiscovery} is used to add a ntp Thing for the local time in the discovery inbox
 * *
 *
 * @author Alexandru-Sever Horin
 */
public class HomeAutoLogicDiscovery extends AbstractDiscoveryService {
    private Logger logger = LoggerFactory.getLogger(HomeAutoLogicDiscovery.class);

    public HomeAutoLogicDiscovery() throws IllegalArgumentException {
        super(SUPPORTED_THING_TYPES_UIDS, 10);
    }

    @Override
    protected void startBackgroundDiscovery() {
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                discoverHAL();
            }
        }, 1, TimeUnit.SECONDS);
    }

    @Override
    protected void startScan() {
        discoverHAL();
    }

    /**
     * Add a ntp Thing for the local time in the discovery inbox
     */
    private void discoverHAL() {

        logger.debug("discover homeautologic");

        InetAddress inet;

        for (int i = 103; i < 110; i++) {
            try {
                inet = InetAddress.getByAddress(new byte[] { (byte) 192, (byte) 168, 0, (byte) i });
                System.out.println("Sending Ping Request to " + inet);
                System.out.println(inet.isReachable(1000) ? "Host is reachable" : "Host is NOT reachable");
                if (inet.isReachable(1000)) {
                    String address = "http://" + inet.getHostAddress() + "/client?command=info";
                    System.out.println(address);

                    String data = HttpUtils.getStringData(address);

                    JsonObject jsonObject = new JsonParser().parse(data).getAsJsonObject();

                    JsonObject jDevice = jsonObject.get("Device").getAsJsonObject();
                    JsonElement jProduct = jDevice.get("product");

                    String product = jProduct.getAsString();

                    System.out.println(product); // John

                    ThingUID uid = null;

                    if (product.equals("Plug")) {
                        uid = new ThingUID(THING_TYPE_SWITCH, "t_h_".concat(Integer.toString(i)));
                    } else if (product.equals("Sensor")) {
                        uid = new ThingUID(THING_TYPE_SENSOR, "pl_".concat(Integer.toString(i)));
                    }

                    if (uid != null) {
                        Map<String, Object> properties = new HashMap<>(4);
                        properties.put("ip", inet.toString().substring(1));
                        DiscoveryResult result = DiscoveryResultBuilder.create(uid).withProperties(properties)
                                .withLabel("t+h discovered").build();
                        thingDiscovered(result);
                    }
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        /*
         * Map<String, Object> properties = new HashMap<>(4);
         * properties.put(PROPERTY_TIMEZONE, TimeZone.getDefault().getID());
         * ThingUID uid = new ThingUID(THING_TYPE_NTP, "local");
         * if (uid != null) {
         * DiscoveryResult result = DiscoveryResultBuilder.create(uid).withProperties(properties)
         * .withLabel("Local Time").build();
         * thingDiscovered(result);
         * }
         */
    }

}
