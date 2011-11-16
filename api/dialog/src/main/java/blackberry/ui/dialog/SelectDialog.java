package blackberry.ui.dialog;

import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.CheckboxField;
import net.rim.device.api.ui.component.ListField;
import net.rim.device.api.ui.component.ListFieldCallback;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.container.PopupScreen;
import net.rim.device.api.ui.container.VerticalFieldManager;

public class SelectDialog extends PopupScreen implements FieldChangeListener, ListFieldCallback {
    private SelectDialog thisDialog;
    private ButtonField doneButton;
    private CheckboxField[] checkboxFieldList;
    
    private int choiceLength = 0;
    private int[] _response = null;
    
    private final String TYPE_SINGLE = "select-single";
    private final String TYPE_MULITPLE = "select-multiple";
    
    private DialogListener _closeListener;
    
    private ListField listChoices;
    private String[] _choices;
    private String _type;
    
    public SelectDialog( String type, String[] choice ) {
        super( new VerticalFieldManager(), Field.FOCUSABLE );
        choiceLength = choice.length;
        thisDialog = this;
        _choices = choice;
        _type = type;

        int numChoices = 0;
        if( type.equals( TYPE_MULITPLE ) ) {
            doneButton = new ButtonField( "DONE", Field.FIELD_HCENTER );
            doneButton.setChangeListener( this );

            checkboxFieldList = new CheckboxField[ choice.length ];
            for( numChoices = 0; numChoices < choice.length; numChoices++ ) {
                checkboxFieldList[ numChoices ] = new CheckboxField( choice[ numChoices ], false );
                add( checkboxFieldList[ numChoices ] );
            }
            add( new SeparatorField() );
            add( doneButton );
        } else if( type.equals( TYPE_SINGLE ) ) {
            listChoices = new ListField( choice.length );
            listChoices.setCallback( this );
            add( listChoices );
        }
    }

    public void setDialogListener( DialogListener dialogListener ) {
        _closeListener = dialogListener;
    }
    
    public void display() {
        new Thread( new Runnable() {

            public void run() {
                UiApplication.getUiApplication().invokeLater( new Runnable() {
                    public void run() {
                        UiApplication.getUiApplication().pushScreen( thisDialog );
                    }
                } );
                while( _response == null )
                    try {
                        Thread.sleep( 250 );
                    } catch( InterruptedException e ) {
                        e.printStackTrace();
                    }

                UiApplication.getUiApplication().invokeLater( new Runnable() {
                    public void run() {
                        UiApplication.getUiApplication().popScreen( thisDialog );
                    }
                } );
                close();
            }

        } ).start();
    }

    public int[] getResponse() {
        return _response;
    }

    public void close() {
        if( _closeListener != null ) {
            _closeListener.onDialogClosed( _response );
        }
    }
    
    public void fieldChanged( Field field, int arg1 ) {
        if( field == doneButton ) {
            int index = 0;
            int counter = 0;
            for( index = 0; index < choiceLength; index++ ) {
                if( checkboxFieldList[ index ].getChecked() ) {
                    counter++;
                }
            }

            int[] response = new int[ counter ];
            int responseIndex = 0;
            for( index = 0; index < choiceLength; index++ ) {
                if( checkboxFieldList[ index ].getChecked() ) {
                    response[ responseIndex ] = index;
                    responseIndex++;
                }
            }
            _response = response;
        }
    }

    protected boolean navigationClick( int status, int time ) {
        if( _type.equals( TYPE_SINGLE ) ) {
            _response = new int[] { listChoices.getSelectedIndex() };
        }
        return super.navigationClick( status, time );
    }

    public void drawListRow( ListField list, Graphics g, int index, int y, int w ) {
        g.setColor( Color.WHITE );
        g.drawText( _choices[ index ], 50, y, 0, w );
    }

    public Object get( ListField list, int index ) {
        _response = new int[] { index };
        return new Integer( index );
    }

    public int getPreferredWidth( ListField arg0 ) {
        return 0;
    }

    public int indexOfList( ListField arg0, String arg1, int arg2 ) {
        return 0;
    }
}