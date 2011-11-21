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
package blackberry.ui.dialog;

import java.util.Vector;

import net.rim.device.api.script.ScriptEngine;
import net.rim.device.api.script.Scriptable;
import net.rim.device.api.script.ScriptableFunction;
import net.rim.device.api.ui.UiApplication;

import blackberry.core.FunctionSignature;
import blackberry.core.ScriptableFunctionBase;

import blackberry.common.util.json4j.JSONArray;

/**
 * Implementation of asynchronous selection dialog
 * 
 * @author jachoi
 * 
 */
public class SelectAsyncFunction extends ScriptableFunctionBase {

    public static final String NAME = "selectAsync";
    
    public static final int POPUP_ITEM_TYPE_OPTION = 0;
    public static final int POPUP_ITEM_TYPE_GROUP = 1;
    public static final int POPUP_ITEM_TYPE_SEPARATOR = 2;
    
    private ScriptEngine _scriptEngine;
    
    public SelectAsyncFunction(ScriptEngine se) {
        _scriptEngine = se;
    }

    /**
     * @see blackberry.core.ScriptableFunctionBase#execute(Object, Object[])
     */
    public Object execute(Object thiz, Object[] args) throws Exception {
        boolean allowMultiple = ((Boolean) args[0]).booleanValue();
        Scriptable choices = (Scriptable) args[1];
        String callback = (String) args[2] ;
        
        int numChoices = choices.getElementCount();
        String[] labels = new String[numChoices];
        boolean[] enabled = new boolean[numChoices];
        boolean[] selected = new boolean[numChoices]; 
        int[] types = new int[numChoices];
        
        populateChoiceStateArrays(choices, labels, enabled, selected, types);

        // create dialog
        SelectDialog d = new SelectDialog(allowMultiple, labels, enabled, selected, types);
        SelectDialogRunner currentDialog = new SelectDialogRunner(d, _scriptEngine, callback);

        // queue
        UiApplication.getUiApplication().invokeLater(currentDialog);

        // return value
        return Scriptable.UNDEFINED;
    }
    
    private void populateChoiceStateArrays(Scriptable fromScriptableChoices, String[] labels, boolean[] enabled, boolean[] selected, int[] type) {
        try {
            
            for(int i = 0; i < fromScriptableChoices.getElementCount(); i++) {
                Scriptable choice = (Scriptable)fromScriptableChoices.getElement(i);
                labels[i] = (String)choice.getField("label");
                enabled[i] = ((Boolean)choice.getField("enabled")).booleanValue();
                selected[i] = ((Boolean)choice.getField("selected")).booleanValue();
                type[i] = ((String)choice.getField("type")).equals("group") ? POPUP_ITEM_TYPE_GROUP : POPUP_ITEM_TYPE_OPTION;
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * @see blackberry.core.ScriptableFunctionBase#getFunctionSignatures()
     */
    protected FunctionSignature[] getFunctionSignatures() {
        FunctionSignature fs = new FunctionSignature(3);
        // allowMultiple
        fs.addParam(Boolean.class, true);
        // choices
        fs.addParam(Scriptable.class, true);
        //callback
        fs.addParam(String.class, true);
        
        return new FunctionSignature[] { fs };
    }

    private static class SelectDialogRunner implements Runnable {
        private SelectDialog _dialog;
        private ScriptEngine _se;
        private String _callback;
            
        /**
         * Constructs a <code>DialogRunner</code> object.
         * 
         * @param dialog
         *            The dialog
         * @param callback
         *            The onSelect callback
         */
        SelectDialogRunner(SelectDialog dialog, ScriptEngine se, String callback) {
                _dialog = dialog;
                _se = se;
                _callback = callback;
        }
        

        /**
         * Run the dialog.
         * 
         * @see java.lang.Runnable#run()
         */
        public void run() { 
            UiApplication.getUiApplication().pushModalScreen(_dialog);
            
            int[] newSelectedItems = _dialog.getResponse();
            
            if(newSelectedItems != null) {
                try {
                    final String jsCallbackArgs = (new JSONArray(boxIntArray(newSelectedItems))).toString();
                    _se.executeScript(_callback + "(" + jsCallbackArgs + ");", null);
                } catch (Exception e) {
                    throw new RuntimeException("Invoke callback failed: " + e.getMessage());
                }
            }
        }
        
        private Integer[] boxIntArray(int[] ints) {
            Integer[] boxed = new Integer[ints.length];
            
            for(int i = 0; i < ints.length; i++) {
                boxed[i] = new Integer(ints[i]);
            }
            
            return boxed;
        }
    }
}
