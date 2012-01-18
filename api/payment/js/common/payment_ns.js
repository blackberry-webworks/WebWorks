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
(function () {
	function Payment(disp) {
		/*
        * Define public static function blackberry.payment.purchase() & 
        */
		this.constructor.prototype.purchase = function(purchaseArgs, callbackOnSuccess, callbackOnFailure) { 
			return disp.purchase(purchaseArgs, callbackOnSuccess, callbackOnFailure); 
		};
		
		/*
		 * Define public static function blackberry.payment.getExistingPurchases()
		 */
		this.constructor.prototype.getExistingPurchases = function(refresh, callbackOnSuccess, callbackOnFailure) {
			return disp.getExistingPurchases(refresh, callbackOnSuccess, callbackOnFailure); 
		};
		
		/*
		 * Define public static function blackberry.payment.getPrice()
		 */		
		this.constructor.prototype.getPrice = function(sku, callbackOnSuccess, callbackOnFailure) { 
			return disp.getPrice(sku, callbackOnSuccess, callbackOnFailure); 
		};
		
		/*
		 * Define public static function blackberry.payment.checkExisting()
		 */	
		this.constructor.prototype.checkExisting = function(sku, callbackOnSuccess, callbackOnFailure) { 
			return disp.checkExisting(sku, callbackOnSuccess, callbackOnFailure); 
		};
		
		/*
		 * Define public static function blackberry.payment.getPurchaseDetails()
		 */	
		this.constructor.prototype.getPurchaseDetails = function(sku, callbackOnSuccess, callbackOnFailure) { 
			return disp.getPurchaseDetails(sku, callbackOnSuccess, callbackOnFailure); 
		};
		
		/*
		 * Define public static function blackberry.payment.cancelSubscription()
		 */	
		this.constructor.prototype.cancelSubscription = function(transactionID, callbackOnSuccess, callbackOnFailure) { 
			return disp.cancelSubscription(transactionID, callbackOnSuccess, callbackOnFailure); 
		};
		
		/*
		 * Define public static function blackberry.payment.updateAppWorld()
		 */	
		this.constructor.prototype.updateAppWorld= function() { 
			return disp.updateAppWorld(); 
		};
		
		/*
		 * Define public static function blackberry.payment.isAppWorldInstalledAndAtCorrectVersion()
		 */	
		this.constructor.prototype.isAppWorldInstalledAndAtCorrectVersion = function( callbackOnSuccess, callbackOnFailure){
			return disp.isAppWorldInstalledAndAtCorrectVersion( callbackOnSuccess, callbackOnFailure);
		};
		
		/*
		 * Define public static function blackberry.payment.setDebugTrue()
		 */	
		this.constructor.prototype.setDebugTrue = function(){
			return disp.PS_DEBUG = true;
		};
		
		/*
		 * Define public static function blackberry.payment.setDebugFalse()
		 */	
		this.constructor.prototype.setDebugFalse = function(){
			return disp.PS_DEBUG = false;
		};
		
		/*
		 * Define public static function blackberry.payment.isPaymentServicesAvailable()
		 */	
		this.constructor.prototype.isPaymentServicesAvailable = function(callbackOnSuccess, callbackOnFailure) {
			return disp.isPaymentServicesAvailable(callbackOnSuccess, callbackOnFailure);
		};
		
		//TODO: execute isPaymentServicesAvailable to add in callbackOnSuccess function:
		this.constructor.prototype.getDigitalGoods = function(callbackOnSuccess, callbackOnFailure) { 
			return disp.getDigitalGoods(callbackOnSuccess, callbackOnFailure); 
		};
		
		//TODO: execute isPaymentServicesAvailable to add in callbackOnSuccess function:
		this.constructor.prototype.getPurchaseHistory = function(callbackOnSuccess, callbackOnFailure) { 
			return disp.getPurchaseHistory(callbackOnSuccess, callbackOnFailure); 
		};
	}
	
	blackberry.Loader.javascriptLoaded("blackberry.payment", Payment);
})();
