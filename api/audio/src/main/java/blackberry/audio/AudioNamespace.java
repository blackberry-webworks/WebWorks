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
package blackberry.audio;

import java.util.Vector;

import javax.microedition.media.Manager;

import net.rim.device.api.script.Scriptable;
import blackberry.common.util.FeaturesHash;
import blackberry.core.FunctionSignature;
import blackberry.core.ScriptableFunctionBase;

public class AudioNamespace extends Scriptable {

    public static final String NAME = "blackberry.audio";

    // The only supported content types.
    private static final String CONTENT_TYPE = "audio/";

    // Not supported protocols.
    private static final String PROTOCOL_CAPTURE = "capture";
    private static final String PROTOCOL_DEVICE = "device";

    private static AudioNamespace _instance = null;

    public static synchronized AudioNamespace getInstance() {
        if (_instance == null) {
            _instance = new AudioNamespace();
        }

        return _instance;
    }

    private AudioNamespace() {
    }

    /* @Override */
    public Object getField(final String name) throws Exception {
        final Object obj = FeaturesHash.getObjectForLoadedFeature(FeaturesHash.formatFeature(AudioNamespace.NAME, name));
        if (obj != null) {
            return obj;
        }

        if (name.equals(SupportedContentTypesFunction.NAME)) {
            return new SupportedContentTypesFunction();
        }
        else if (name.equals(SupportedProtocolsFunction.NAME)) {
            return new SupportedProtocolsFunction();
        }

        return super.getField(name);
    }

    // Return the list of supported content types for the given protocol
    public class SupportedContentTypesFunction extends ScriptableFunctionBase {
        public static final String NAME = "supportedContentTypes";

		protected Object execute(Object thiz, Object[] args) throws Exception {
			try {

                if (args==null || isProtocolSupportedByExtension((String) args[0])) {
                    return getItemsSupportedByExtension(Manager.getSupportedContentTypes(args == null ? null : (String) args[0]), true);
                }
                else {
                    return new String[0];
                }
            } catch (final Exception e) {
            }
            return UNDEFINED;
		}

		protected FunctionSignature[] getFunctionSignatures() {
			FunctionSignature fSig = new FunctionSignature(1);
			fSig.addNullableParam(String.class, true);
			return new FunctionSignature[] {fSig};
		}
		
		
    }

    // Return the list of supported protocols given the content type
    public class SupportedProtocolsFunction extends ScriptableFunctionBase {
        public static final String NAME = "supportedProtocols";

        /* @Override */
        public Object execute(final Object thiz, final Object[] args) throws Exception {
                try {
                    if (args == null || isContentTypeSupportedByExtension((String) args[0])) {
                        return getItemsSupportedByExtension(Manager.getSupportedProtocols(args == null ? null : (String) args[0]), false);
                    }
                    else {
                        return new String[0];
                    }

                } catch (final Exception e) {
                }

            return UNDEFINED;
        }
        
		protected FunctionSignature[] getFunctionSignatures() {
			FunctionSignature fSig = new FunctionSignature(1);
			fSig.addNullableParam(String.class, true);
			return new FunctionSignature[] {fSig};
		}
    }

    private boolean isContentTypeSupportedByExtension(final String contentType) {
        if (contentType != null && !contentType.trim().startsWith(CONTENT_TYPE)) {
            return false;
        }
        else {
            return true;
        }
    }

    private boolean isProtocolSupportedByExtension(final String protocol) {
        if (protocol != null && (protocol.trim().equals(PROTOCOL_CAPTURE) || protocol.trim().equals(PROTOCOL_DEVICE))) {
            return false;
        }
        else {
            return true;
        }
    }

    private String[] getItemsSupportedByExtension(final String[] supportedByDevice, final boolean contentType) {
        final Vector protocolVector = new Vector();
        for (int i = 0; i < supportedByDevice.length; i++) {
            if (contentType && isContentTypeSupportedByExtension(supportedByDevice[i]) || !contentType
                    && isProtocolSupportedByExtension(supportedByDevice[i])) {
                protocolVector.addElement(supportedByDevice[i]);
            }
        }

        final String[] supportedByExtension = new String[protocolVector.size()];
        protocolVector.copyInto(supportedByExtension);

        return supportedByExtension;
    }

}
