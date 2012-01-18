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
package blackberry.payment;

import java.util.Vector;

import net.rim.device.api.browser.field2.BrowserField;
import net.rim.device.api.script.ScriptEngine;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.EventLogger;
import net.rim.device.api.util.SimpleSortingVector;
import net.rim.device.api.web.WidgetConfig;
import net.rim.device.api.web.WidgetException;
import net.rimlib.blackberry.api.paymentsdk.DigitalGoodNotFoundException;
import net.rimlib.blackberry.api.paymentsdk.ExistingPurchasesResult;
import net.rimlib.blackberry.api.paymentsdk.IllegalApplicationException;
import net.rimlib.blackberry.api.paymentsdk.PaymentEngine;
import net.rimlib.blackberry.api.paymentsdk.PaymentEngineListener;
import net.rimlib.blackberry.api.paymentsdk.PaymentException;
import net.rimlib.blackberry.api.paymentsdk.PaymentServerException;
import net.rimlib.blackberry.api.paymentsdk.PriceSet;
import net.rimlib.blackberry.api.paymentsdk.Purchase;
import net.rimlib.blackberry.api.paymentsdk.PurchaseArgumentsBuilder;
import net.rimlib.blackberry.api.paymentsdk.Result;
import net.rimlib.blackberry.api.paymentsdk.digitalGoods.DigitalGood;
import net.rimlib.blackberry.api.paymentsdk.digitalGoods.DigitalGoods;
import net.rimlib.blackberry.api.paymentsdk.digitalGoods.DigitalGoodsListingListener;
import net.rimlib.blackberry.api.paymentsdk.purchaseHistory.PurchaseHistory;
import net.rimlib.blackberry.api.paymentsdk.purchaseHistory.PurchaseHistoryListingListener;

import org.w3c.dom.Document;

import blackberry.common.util.JSUtilities;
import blackberry.common.util.json4j.JSONArray;
import blackberry.common.util.json4j.JSONException;
import blackberry.common.util.json4j.JSONObject;
import blackberry.core.IJSExtension;
import blackberry.core.JSExtensionRequest;
import blackberry.core.JSExtensionResponse;
import blackberry.core.JSExtensionReturnValue;

/**
 * JavaScript extension for blackberry.payment
 */
public class PaymentExtension implements IJSExtension {
    private static Vector SUPPORTED_METHODS;
    
    //	public PurchaseResult purchase(PurchaseArguments arguments)
    public static final String FUNCTION_PURCHASE = "purchase";
    
    //  public ExistingPurchasesResult getExistingPurchases(boolean allowRefresh)
    public static final String FUNCTION_GETEXISTINGPURCHASES = "getExistingPurchases";

    // No supported in 1.5
    public static final String FUNCTION_GETMODE = "getDevelopmentMode";
    public static final String FUNCTION_SETMODE = "setDevelopmentMode";
    public static boolean developmentMode = true;
    
    
    //gpoor 1.5 PS SDK methods
    // public Result cancel(String transactionID)
    public static final String FUNCTION_CANCEL = "cancel";
    // public PriceSet getPrice(String sku)
    public static final String FUNCTION_GETPRICE = "getPrice";
    // public static void isAppWorldInstalledAndAtCorrectVersion() throws AppWorldUpdateRequired 
    public static final String FUNCTION_ISAPPWORLDINSTALLEDANDATCORRECTVERSION = "isAppWorldInstalledAndAtCorrectVersion";
    // public static void upDateAppWorld()
    public static final String FUNCTION_UPDATEAPPWORLD ="updateAppWorld";
    // public boolean checkExisting(String sku) 
    public static final String FUNCTION_CHECKEXISTING ="checkExisting";
    // public Purchase get(String sku) 
    public static final String FUNCTION_GET = "get"; //

    //mwaugh 2.0 PS SDK methods
    // public static void isPaymentServicesAvaliable( PaymentEngineListener listener ) 
    public static final String FUNCTION_ISPAYMENTSERVICESAVAILABLE="isPaymentServicesAvaliable";
    public static final String FUNCTION_GETDIGITALGOODS="getDigitalGoods";
    public static final String FUNCTION_GETPURCHASEHISTORY="getPurchaseHistory";

    public static final String KEY_DG_ID = "digitalGoodID";
    public static final String KEY_DG_SKU = "digitalGoodSKU";
    public static final String KEY_DG_NAME = "digitalGoodName";
    public static final String KEY_METADATA = "metaData";
    public static final String KEY_APP_NAME = "purchaseAppName";
    public static final String KEY_APP_ICON = "purchaseAppIcon";
    public static final String KEY_DG_TRANSACTIONID = "transactionID";

    public static final String KEY_REFRESH = "allowRefresh";

    private static final int CODE_USER_CANCEL = 1;
    private static final int CODE_SYS_BUSY = 2;
    private static final int CODE_SYS_ERROR = 3;
    private static final int CODE_NOT_FOUND = 4;
    private static final int CODE_ILLEGAL_APP = 5;

    private PaymentEngineListener _paymentEngineListener;
    private PurchaseHistoryListingListener _purchaseHistoryListener;
    private DigitalGoodsListingListener _digitalGoodsListener;
    private boolean _callbackReturned = false;
    
    static {
        SUPPORTED_METHODS = new Vector();
        SUPPORTED_METHODS.addElement( FUNCTION_PURCHASE );
        SUPPORTED_METHODS.addElement( FUNCTION_GETEXISTINGPURCHASES );
        SUPPORTED_METHODS.addElement( FUNCTION_GETMODE );
        SUPPORTED_METHODS.addElement( FUNCTION_SETMODE );
        // 1.5 methods..
        SUPPORTED_METHODS.addElement( FUNCTION_CANCEL );
        SUPPORTED_METHODS.addElement( FUNCTION_GETPRICE );
        SUPPORTED_METHODS.addElement( FUNCTION_ISAPPWORLDINSTALLEDANDATCORRECTVERSION );
        SUPPORTED_METHODS.addElement( FUNCTION_UPDATEAPPWORLD );
        SUPPORTED_METHODS.addElement( FUNCTION_CHECKEXISTING );
        SUPPORTED_METHODS.addElement( FUNCTION_GET );
        // 2.0 method
        SUPPORTED_METHODS.addElement( FUNCTION_ISPAYMENTSERVICESAVAILABLE );
        SUPPORTED_METHODS.addElement( FUNCTION_GETDIGITALGOODS );
        SUPPORTED_METHODS.addElement( FUNCTION_GETPURCHASEHISTORY );
    }
    
    private static String[] JS_FILES = { "payment_dispatcher.js", "payment_ns.js" };

    public String[] getFeatureList() {
        return new String[] { "blackberry.payment" };
    }

    /**
     * Implements invoke() of interface IJSExtension. Methods of extension will be called here.
     * 
     * @throws WidgetException
     *             if specified method cannot be recognized
     */
    public void invoke( final JSExtensionRequest request, final JSExtensionResponse response ) throws WidgetException {
        final long eventLogGuid = 0x56e6b31c303f090aL;
    	EventLogger.register( eventLogGuid, "net_rim_bb_payment_webworks_app", EventLogger.VIEWER_STRING );
    	
    	String method = request.getMethodName();
        Object[] args = request.getArgs();
        String msg = "";
        int code = JSExtensionReturnValue.SUCCESS;
        JSONObject data = new JSONObject();
        JSONArray dataArray = new JSONArray();
        JSONObject returnValue = null;

        PaymentEngine engine = PaymentSystem.getInstance();

        if( engine == null ) {
            throw new IllegalArgumentException(
                    "Sorry, in-app purchases are unavailable. Make sure BlackBerry App World v2.1 or higher is installed on your device." );
        }

        if( !SUPPORTED_METHODS.contains( method ) ) {
            throw new WidgetException( "Undefined method: " + method );
        }

        try {
            if( method.equals( FUNCTION_PURCHASE ) ) {
                String digitalGoodID = (String) request.getArgumentByName( KEY_DG_ID );
                String digitalGoodSKU = (String) request.getArgumentByName( KEY_DG_SKU );
                String digitalGoodName = (String) request.getArgumentByName( KEY_DG_NAME );
                String metaData = (String) request.getArgumentByName( KEY_METADATA );
                String purchaseAppName = (String) request.getArgumentByName( KEY_APP_NAME );
                String purchaseAppIcon = (String) request.getArgumentByName( KEY_APP_ICON );

                PurchaseArgumentsBuilder arguments = new PurchaseArgumentsBuilder().withDigitalGoodId( digitalGoodID )
                        .withDigitalGoodName( digitalGoodName ).withDigitalGoodSku( digitalGoodSKU ).withMetadata( metaData )
                        .withPurchasingAppName( purchaseAppName )
                        .withPurchasingAppIcon( ( purchaseAppIcon != null ? Bitmap.getBitmapResource( purchaseAppIcon ) : null ) );

                // Blocking call: engine.purchase() invokes AppWorld screen.
                Purchase successfulPurchase = engine.purchase( arguments.build() );
                String purchaseJSON = purchaseToJSONString( successfulPurchase );

                data = new JSONObject( purchaseJSON );
                code = JSExtensionReturnValue.SUCCESS;
                msg = "Purchase Successful";
            } else if( method.equals( FUNCTION_GETEXISTINGPURCHASES ) ) {
                String refresh = (String) request.getArgumentByName( KEY_REFRESH );
                // Blocking call: engine.getExistingPurchases() invokes AppWorld screen.
                ExistingPurchasesResult existingPurchases = engine.getExistingPurchases( parseBoolean( refresh ) );
                Purchase[] purchases = existingPurchases.getPurchases();
                if( purchases.length != 0 ) {
                    for( int i = 0; i < purchases.length; i++ ) {
                        String purchaseJSON = "";
                        purchaseJSON += purchaseToJSONString( purchases[ i ] );
                        JSONObject temp = new JSONObject( purchaseJSON );
                        dataArray.add( temp );
                    }
                }
            } else if( method.equals( new String( FUNCTION_GETMODE ) ) ) {
                data.put( "developmentMode", PaymentSystem.getMode() );
                code = JSExtensionReturnValue.SUCCESS;
                msg = "developmentMode set to: " + PaymentSystem.getMode();
            } else if( method.equals( new String( FUNCTION_SETMODE ) ) ) {
                String s = (String) request.getArgumentByName( "developmentMode" );
                
                if( s.equals( "true" ) ) {
             //       PaymentSystem.setMode( true );
                } else {
           ///         PaymentSystem.setMode( false );
                }
                code = JSExtensionReturnValue.SUCCESS;
                msg = "developmentMode set to: " + s;
                throw new PaymentException("gpoor: not supported in 1.5 legacy function ");
            } else if( method.equals( FUNCTION_GETPRICE ) ) {
            	  String digitalGoodSKU = (String) request.getArgumentByName( KEY_DG_SKU );
                  // Blocking call: engine.getExistingPurchases() invokes AppWorld screen.
                  PriceSet priceSet = engine.getPrice( digitalGoodSKU );
                  String priceSetJSON = "";
                  priceSetJSON += priceSetToJSONString( priceSet );
                  data = new JSONObject( priceSetJSON );
                  code = JSExtensionReturnValue.SUCCESS;
                  msg = "Got Price of SKU="+digitalGoodSKU;
            } else  if( method.equals( FUNCTION_CANCEL ) ) {
                String transactionID = (String) request.getArgumentByName( KEY_DG_TRANSACTIONID );
                // Blocking call: engine.cancel() invokes AppWorld screen.
                Result result = engine.cancel( transactionID );
                String resultJSON = resultToJSONString( result );
                data = new JSONObject( resultJSON );
                code = JSExtensionReturnValue.SUCCESS;
                msg = "Canceled Item with transactionID="+transactionID;
            }  else  if( method.equals( FUNCTION_CHECKEXISTING ) ) {
                String digitalGoodSKU = (String) request.getArgumentByName( KEY_DG_SKU );
                // Blocking call: engine.checkExisting() invokes AppWorld screen.
                boolean isSKUPurchased = engine.checkExisting( digitalGoodSKU );
                data = new JSONObject( isSKUPurchased ? "true":"false" );
                code = JSExtensionReturnValue.SUCCESS;
                msg = "Checked  SKU="+digitalGoodSKU+" purchased="+isSKUPurchased;
            } else  if( method.equals( FUNCTION_GET ) ) {
                String digitalGoodSKU = (String) request.getArgumentByName( KEY_DG_SKU );
                // Blocking call: engine.get() invokes AppWorld screen.
                Purchase successfulPurchase = engine.get( digitalGoodSKU );
                String purchaseJSON = purchaseToJSONString( successfulPurchase );
                data = new JSONObject( purchaseJSON );
                code = JSExtensionReturnValue.SUCCESS;
                msg = "Got Item with SKU="+digitalGoodSKU;
            }  else  if( method.equals( FUNCTION_ISAPPWORLDINSTALLEDANDATCORRECTVERSION ) ) {
                PaymentEngine.isAppWorldInstalledAndAtCorrectVersion();            
                code = JSExtensionReturnValue.SUCCESS;
                msg = "Checked that AppWorld is installed at "+PaymentEngine.MINIMUM_REQUIRED_APP_WORLD_VERSION;
            }  else  if( method.equals( FUNCTION_UPDATEAPPWORLD ) ) {
        
                // Blocking call: engine.checkExisting() invokes AppWorld screen.
                PaymentEngine.upDateAppWorld();
                code = JSExtensionReturnValue.SUCCESS;
                msg = "Will open browser to updated AppWorld";
            }  else if (method.equals( FUNCTION_ISPAYMENTSERVICESAVAILABLE ) ) {
            	_callbackReturned = false;
            	
            	_paymentEngineListener = new PaymentEngineListener() {
					private int retCode = JSExtensionReturnValue.SUCCESS;
					private String retMsg = "";
					JSONObject retData = new JSONObject();
					
					public void error(String message, int errorCode) {
						retCode = JSExtensionReturnValue.FAIL;
						retMsg = "Message: " + message + ", code: " + errorCode;
						try {
							retData.put("errorCode", errorCode + "");
						} catch (JSONException e) {
							e.printStackTrace();
						}
						JSONObject returnVal = new JSExtensionReturnValue( retMsg, retCode, retData ).getReturnValue();
						response.setPostData( returnVal.toString().getBytes() );
						_callbackReturned = true;
					}
					
					public void success() {
						retCode = JSExtensionReturnValue.SUCCESS;
						retMsg = "Payment Services are available";
						try {
							retData.put("errorCode", "-1");
						} catch (JSONException e) {
							e.printStackTrace();
						}
						JSONObject returnVal = new JSExtensionReturnValue( retMsg, retCode, retData ).getReturnValue();
						String strResponse = returnVal.toString();
						response.setPostData( strResponse.getBytes() );
						_callbackReturned = true;
					}
				};
				
            	PaymentEngine.isPaymentServicesAvaliable( _paymentEngineListener  );
            	while(!_callbackReturned) {
            		// waiting for the callback to complete
            	}
            } else if (method.equals(FUNCTION_GETDIGITALGOODS)) {
            	_callbackReturned = false;
            	
				_digitalGoodsListener = new DigitalGoodsListingListener() {
					private int retCode = JSExtensionReturnValue.SUCCESS;
					private String retMsg = "";
					JSONObject retData = new JSONObject();
					
					public void error(String message, int errorCode) {
						retCode = JSExtensionReturnValue.FAIL;
						retMsg = "Message: " + message + ", code: " + errorCode;
						try {
							retData.put("errorCode", errorCode + "");
						} catch (JSONException e) {
							e.printStackTrace();
						}
						JSONObject returnVal = new JSExtensionReturnValue( retMsg, retCode, retData ).getReturnValue();
						response.setPostData( returnVal.toString().getBytes() );
						_callbackReturned = true;
					}
					
					public void success(DigitalGood[] goods) {
						retCode = JSExtensionReturnValue.SUCCESS;
						retMsg = "Success";
						JSONArray retDataArray = new JSONArray();
						
						if(goods.length > 0) {
							for (int i = 0; i < goods.length; i ++ ) {
								try {
									String dgAsString = digitalGoodToJSON(goods[i]);
									JSONObject temp = new JSONObject( dgAsString );
									retDataArray.add( temp );
								} catch (JSONException e) {
									e.printStackTrace();
								}
							}
						}
						
						JSONObject returnVal = new JSExtensionReturnValue( retMsg, retCode, retDataArray ).getReturnValue();
						EventLogger.logEvent( eventLogGuid, ("[DigitalGoods.get()] Return value: " + returnVal.toString()).getBytes());
						response.setPostData( returnVal.toString().getBytes() );
						_callbackReturned = true;
					}
				};
				
				DigitalGoods.get(_digitalGoodsListener);
				
				while(!_callbackReturned) {
            		// waiting for the callback to complete
            	}
				
            } else if (method.equals(FUNCTION_GETPURCHASEHISTORY)) {
            	_callbackReturned = false;
            	_purchaseHistoryListener = new PurchaseHistoryListingListener() {
            		private int retCode = JSExtensionReturnValue.SUCCESS;
					private String retMsg = "";
					JSONObject retData = new JSONObject();
					
					public void error(String message, int errorCode) {
						retCode = JSExtensionReturnValue.FAIL;
						retMsg = "Message: " + message + ", code: " + errorCode;
						try {
							retData.put("errorCode", errorCode + "");
						} catch (JSONException e) {
							EventLogger.logEvent( eventLogGuid, e.getMessage().getBytes());
						}
						JSONObject returnVal = new JSExtensionReturnValue( retMsg, retCode, retData ).getReturnValue();
						response.setPostData( returnVal.toString().getBytes() );
						_callbackReturned = true;
					}
					
					public void success(Purchase[] purchases) {
						retCode = JSExtensionReturnValue.SUCCESS;
						
						JSONArray retDataArray = new JSONArray();
						if( purchases.length > 0 ) {
		                    for( int i = 0; i < purchases.length; i++ ) {
								try {
									String purchaseJSON = purchaseToJSONString( purchases[ i ] );
									JSONObject temp = new JSONObject( purchaseJSON );
									retDataArray.add( temp );
								} catch (JSONException e) {
									EventLogger.logEvent( eventLogGuid, e.getMessage().getBytes());
								}
		                    }
		                }
						JSONObject returnVal = new JSExtensionReturnValue( retMsg, retCode, retDataArray ).getReturnValue();
						EventLogger.logEvent( eventLogGuid, ("[PurchaseHistory.get()] Return value: " + returnVal.toString()).getBytes());
						response.setPostData( returnVal.toString().getBytes() );
						_callbackReturned = true;
					}
				};
            	PurchaseHistory.get(_purchaseHistoryListener);
            	
            	while(!_callbackReturned) {
            		// waiting for the callback to complete
            	}
            }
        } catch( DigitalGoodNotFoundException e ) {
            code = CODE_NOT_FOUND;
            msg = e.getMessage();
        } catch( PaymentServerException e ) {
            code = CODE_SYS_ERROR;
            msg = e.getMessage();
        } catch( IllegalApplicationException e ) {
            code = CODE_ILLEGAL_APP;
            msg = e.getMessage();
        } catch( PaymentException e ) {
            code = CODE_SYS_ERROR;
            msg = e.getMessage();
        } catch( Exception e ) {
            code = JSExtensionReturnValue.FAIL;
            msg = e.getMessage();
        }

        if( method.equals( FUNCTION_GETEXISTINGPURCHASES ) ) {
            returnValue = new JSExtensionReturnValue(  msg, code, dataArray ).getReturnValue();
            EventLogger.logEvent( eventLogGuid, ("[getExistingPurchases()] Return value: " + returnValue.toString()).getBytes());
        } else {
            returnValue = new JSExtensionReturnValue( msg, code, data ).getReturnValue();
        }
        
        if( !( method.equals( FUNCTION_ISPAYMENTSERVICESAVAILABLE ) 
        		|| method.equals( FUNCTION_GETPURCHASEHISTORY ) 
        		|| method.equals( FUNCTION_GETDIGITALGOODS ) ) 
        		) {
        	response.setPostData( returnValue.toString().getBytes() );
        } else {
        	// response will be sent back in call back asynchronously
        }
    }
    
    private static boolean parseBoolean( String str ) {
        return ( str != null && str.equals( Boolean.TRUE.toString() ) );
    }
    
	public String digitalGoodToJSON(DigitalGood digitalGood) {
		 StringBuffer buffer = new StringBuffer();
		 buffer.append( "{" );
		 buffer.append( "\"sku\":\"" + digitalGood.getSKU() + "\"," );
		 buffer.append( "\"name\":\"" + digitalGood.getSKU() + "\"," );
		 buffer.append( "\"longDescription\":\"" + digitalGood.getLongDescription() + "\"," );
		 buffer.append( "\"shortDescription\":\"" + digitalGood.getShortDescription() + "\"," );
		 buffer.append( "\"vendor\":\"" + digitalGood.getVendor() + "\"," );
		 buffer.append( "\"price\":\"" + digitalGood.getPriceSet().getPriceSetValue(PriceSet.PRICE) + "\"," );
		 buffer.append( "\"subscriptionInitialPeriod\":\"" + digitalGood.getPriceSet().getPriceSetValue(PriceSet.SUBSCRIPTION_INITIAL_PERIOD) + "\"," );
		 buffer.append( "\"subscriptionPeriodName\":\"" + digitalGood.getPriceSet().getPriceSetValue(PriceSet.SUBSCRIPTION_PERIOD_NAME) + "\"," );
		 buffer.append( "\"subscriptionRenewalPrice\":\"" + digitalGood.getPriceSet().getPriceSetValue(PriceSet.SUBSCRIPTION_RENEWAL_PRICE) + "\"," );
		 buffer.append( "}" );
		 return buffer.toString();
	}
	
    public String resultToJSONString( Result obj ) {
        StringBuffer buffer = new StringBuffer();
        buffer.append( "{" );
        buffer.append( "\"getFailureReason\":\"" + obj.getFailureReason() + "\"," );
        buffer.append( "\"STATUS_MESSAGE\":\"" + obj.getStatusMessage() + "\"," );
        buffer.append( "}" );
        return buffer.toString();
    }
    
    public String purchaseToJSONString( Purchase obj ) {
        StringBuffer buffer = new StringBuffer();
        buffer.append( "{" );
        buffer.append( "\"digitalGoodID\":\"" + obj.getDigitalGoodId() + "\"," );
        buffer.append( "\"digitalGoodSKU\":\"" + obj.getDigitalGoodSku() + "\"," );
        buffer.append( "\"date\":\"" + Long.toString( obj.getDate().getTime() ) + "\"," );
        buffer.append( "\"licenseKey\":\"" + obj.getLicenseKey() + "\"," );
        buffer.append( "\"metaData\":\"" + obj.getMetadata() + "\"," );
        buffer.append( "\"transactionID\":\"" + obj.getTransactionId() + "\"," );
        buffer.append( "\"subscriptionStartDate\":\"" + obj.getStartDate() + "\"," );
        buffer.append( "\"subscriptionEndDate\":\"" + obj.getEndDate() + "\"," );
        buffer.append( "\"subscriptionInitialPeriod\":\"" + obj.getInitialSubscriptionPeriod() + "\"" );
        buffer.append( "}" );
        return buffer.toString();
    }
    
    public String priceSetToJSONString( PriceSet obj ) {
        StringBuffer buffer = new StringBuffer();
        buffer.append( "{" );
        buffer.append( "\"getFailureReason\":\"" + obj.getFailureReason() + "\"," );
        buffer.append( "\"price\":\"" + obj.getPriceSetValue(PriceSet.PRICE) + "\"," );
        buffer.append( "\"returnCode\":\"" + obj.getPriceSetValue(PriceSet.RETURN_CODE) + "\"," );
        buffer.append( "\"statusMessage\":\"" + obj.getStatusMessage() + "\"," );
        buffer.append( "\"subscriptionInitialPeriod\":\"" + obj.getPriceSetValue(PriceSet.SUBSCRIPTION_INITIAL_PERIOD) + "\"," );
        buffer.append( "\"subscriptionInitialPrice\":\"" + obj.getPriceSetValue(PriceSet.SUBSCRIPTION_INITIAL_PRICE) + "\"," );
        buffer.append( "\"subscriptionPeriodName\":\"" + obj.getPriceSetValue(PriceSet.SUBSCRIPTION_PERIOD_NAME) + "\"," );
        buffer.append( "\"subscriptionRenewalPrice\":\"" + obj.getPriceSetValue(PriceSet.SUBSCRIPTION_RENEWAL_PRICE) + "\"," );     
        buffer.append( "}" );
        return buffer.toString();
    }
    public void loadFeature( String feature, String version, Document document, ScriptEngine scriptEngine,
            SimpleSortingVector jsInjectionPaths ) {
        JSUtilities.loadJS( scriptEngine, JS_FILES, jsInjectionPaths );        
    }


    public void register( WidgetConfig widgetConfig, BrowserField browserField ) {
        
    }

    public void unloadFeatures() {
        
    }
}
