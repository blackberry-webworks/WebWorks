/*
* Copyright 2010-2011 Research In Motion Limited.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package blackberry.message.util;

import net.rim.device.api.system.RadioInfo;

/**
 * Convenient utilities for underlying Java side
 * 
 * @author oel
 * 
 */
public class MessageUtil {

    /**
     * Determines whether the currently active WAF is CDMA
     * 
     * @return True if currently active WAF is CDMA, otherwise false
     */
    public static boolean isCDMA() {
        return ( RadioInfo.getActiveWAFs() & RadioInfo.WAF_CDMA ) == RadioInfo.WAF_CDMA;
    }

    /**
     * Gets the formatted address of the given message.
     * 
     * @param protocol The message protocol
     * @param address The message address
     * @return The formatted address of the message
     */
    public static String getFormattedAddress( String protocol, String address ) {
        if( address == null ) {
            return "";
        }

        String addr = address;
        if( addr.startsWith( protocol ) ) {
            addr = addr.substring( protocol.length() );
        }

        int offset = 0;
        char c = addr.charAt( offset );

        while( !Character.isDigit( c ) ) {
            offset++;
            c = addr.charAt( offset );
        }

        return addr.substring( offset );
    }
}
