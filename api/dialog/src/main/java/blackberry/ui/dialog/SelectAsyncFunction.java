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
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.BitmapField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.container.DialogFieldManager;
import blackberry.core.FunctionSignature;
import blackberry.core.ScriptableFunctionBase;

/**
 * Implementation of asynchronous selection dialog
 * 
 * @author dmeng
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
		int defaultChoice = 0;
		boolean global = false;

		// select type: 'single' or 'multiple'
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
		Dialog d = new Dialog("", choices, values, defaultChoice,
				null /* bitmap */, global ? Dialog.GLOBAL_STATUS : 0 /* style */);
		SelectDialogRunner currentDialog = new SelectDialogRunner(d, callback);

		// queue
		UiApplication.getUiApplication().invokeLater(currentDialog);

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
		private Dialog _dialog;
		private ScriptableFunction _callback;

		/**
		 * Constructs a <code>DialogRunner</code> object.
		 * 
		 * @param dialog
		 *            The dialog
		 * @param callback
		 *            The onSelect callback
		 */
		public SelectDialogRunner(Dialog dialog, ScriptableFunction callback) {
			_dialog = dialog;
			_callback = callback;
			Bitmap bitmap = Bitmap.getPredefinedBitmap(Bitmap.QUESTION);
			setIcon(bitmap);
		}

		/**
		 * Run the dialog.
		 * 
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			int ret = _dialog.doModal();
			try {
				_callback.invoke(_callback,
						new Object[] { new Integer[] { new Integer(ret) } });
			} catch (Exception e) {
				throw new RuntimeException("Invoke callback failed");
			}
		}

		private void setIcon(Bitmap image) {
			BitmapField field = null;
			if (image != null) {
				field = new BitmapField(null, BitmapField.VCENTER
						| BitmapField.STAMP_MONOCHROME);
				field.setBitmap(image);
			}
			DialogFieldManager dfm = (DialogFieldManager) _dialog.getDelegate();
			dfm.setIcon(field);
		}
	}

}
