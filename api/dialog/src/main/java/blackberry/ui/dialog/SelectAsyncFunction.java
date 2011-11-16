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

import net.rim.device.api.script.Scriptable;
import net.rim.device.api.script.ScriptableFunction;
import blackberry.core.FunctionSignature;
import blackberry.core.ScriptableFunctionBase;

/**
 * Implementation of asynchronous selection dialog
 * 
 * @author jachoi
 * 
 */
public class SelectAsyncFunction extends ScriptableFunctionBase {

	public static final String NAME = "selectAsync";

	/**
	 * @see blackberry.core.ScriptableFunctionBase#execute(Object, Object[])
	 */
	public Object execute(Object thiz, Object[] args) throws Exception {
		String type;
		String[] choices;
		int[] values;

		// select type: 'select-single' or 'select-multiple'
		type = (String) args[0];

		// choices & values
		Scriptable stringArray = (Scriptable) args[1];
		int count = stringArray.getElementCount();
		choices = new String[count];
		values = new int[count];
		for (int i = 0; i < count; i++) {
			choices[i] = stringArray.getElement(i).toString();
			values[i] = i;
		}

		ScriptableFunction callback = (ScriptableFunction) args[2];

		// create dialog
		SelectDialog d = new SelectDialog(type, choices);
		SelectDialogRunner currentDialog = new SelectDialogRunner(d, callback);

		// queue
		new Thread(currentDialog).start();

		// return value
		return null;
	}

	/**
	 * @see blackberry.core.ScriptableFunctionBase#getFunctionSignatures()
	 */
	protected FunctionSignature[] getFunctionSignatures() {
		FunctionSignature fs = new FunctionSignature(3);
		// message
		fs.addParam(String.class, true);
		// choices
		fs.addParam(Scriptable.class, true);
		// callback
		fs.addParam(ScriptableFunction.class, true);
		return new FunctionSignature[] { fs };
	}

	private static class SelectDialogRunner implements Runnable {
	    private SelectDialog _dialog;
		private ScriptableFunction _callback;
		
		Object dialogLockObj = new Object();
		
        DialogListener dcl = new DialogListener() {
            public void onDialogClosed( int[] choice ) {
                synchronized( dialogLockObj ) {
                    dialogLockObj.notifyAll();
                }
            }
        };
		
		/**
		 * Constructs a <code>DialogRunner</code> object.
		 * 
		 * @param dialog
		 *            The dialog
		 * @param callback
		 *            The onSelect callback
		 */
		public SelectDialogRunner(SelectDialog dialog, ScriptableFunction callback) {
			_dialog = dialog;
			_callback = callback;
		}
		

		/**
		 * Run the dialog.
		 * 
		 * @see java.lang.Runnable#run()
		 */
		public void run() { 
		    _dialog.setDialogListener( dcl );
		    _dialog.display();

			try {
			   synchronized (dialogLockObj) {
		          dialogLockObj.wait();
		      }
			   int[] ret = _dialog.getResponse();
				_callback.invoke(_callback, new Object[] { intToObjectArray(ret) } );
			} catch (Exception e) {
				throw new RuntimeException("Invoke callback failed");
			}
		}
		
		public Integer[] intToObjectArray(int[] ret) {
		    
		    int index;
		    Integer[] intObjArray = new Integer[ret.length];
		    for (index = 0; index < ret.length; index++) {
		        intObjArray[index] = new Integer( ret[index] );
		    }
		    
		    return intObjArray;
		}
	}
}