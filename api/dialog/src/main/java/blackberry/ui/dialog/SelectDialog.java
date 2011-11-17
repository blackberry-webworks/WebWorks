package blackberry.ui.dialog;

import net.rim.device.api.system.Characters;
import net.rim.device.api.system.Display;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.TouchEvent;
import net.rim.device.api.ui.TouchGesture;
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
    private VerticalFieldManager _vfm;
    private ListItem[] _listItem;
    
    private int choiceLength = 0;
    private int[] _response = null;
    
    private DialogListener _closeListener;
    
    private ListField listChoices;
    private String[] _choices;
    private boolean _allowMultiple;
    
    public SelectDialog(boolean allowMultiple, String[] labels, boolean[] enableds, boolean[] selecteds) {
        super( new PopupDelegate(allowMultiple) );
        choiceLength = labels.length;
        thisDialog = this;
        _choices = labels;
        _allowMultiple = allowMultiple;
        
        
        _vfm = new VerticalFieldManager( NO_HORIZONTAL_SCROLL | NO_HORIZONTAL_SCROLLBAR | VERTICAL_SCROLL | VERTICAL_SCROLLBAR );
        
        _listItem = new ListItem[choiceLength];
        for (int index = 0; index < choiceLength; index++) {
            _listItem[index] = new ListItem(labels[index], false);
        }

        int numChoices = 0;
        if( _allowMultiple ) {
            doneButton = new ButtonField( "DONE", Field.FIELD_HCENTER );
            doneButton.setChangeListener( this );

            checkboxFieldList = new CheckboxField[ choiceLength ];
            for( numChoices = 0; numChoices < choiceLength; numChoices++ ) {
                if ( _listItem[numChoices].isSelected() ) {
                    checkboxFieldList[ numChoices ] = new CheckboxField( " " + _listItem[numChoices].toString(), true );
                } else {
                    checkboxFieldList[ numChoices ] = new CheckboxField( " " + _listItem[numChoices].toString(), false );
                }
                _vfm.add( checkboxFieldList[ numChoices ] );
            }
            add(_vfm);
            add( new SeparatorField() );
            add( doneButton );
        } else if( !_allowMultiple ) {
            listChoices = new ListField( choiceLength );
            listChoices.setCallback( this );
            _vfm.add( listChoices );
            add(_vfm);
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
        if( !_allowMultiple ) {
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
        return Display.getWidth();
    }

    public int indexOfList( ListField arg0, String arg1, int arg2 ) {
        return -1;
    }
    
    private void updateCurrentSelection(char keyChar) {
        // Ensure we were passed a valid key to locate.
        if( keyChar == '\u0000' ) {
            return;
        }
    }
      
    /* @Override */ 
    protected boolean touchEvent( TouchEvent message ) {
        switch (message.getEvent()) {
            case TouchEvent.GESTURE:
                if ( _allowMultiple && message.getGesture().getEvent() == TouchGesture.NAVIGATION_SWIPE ) {
                    int swipeDirection = message.getGesture().getSwipeDirection();
                    Field field = getLeafFieldWithFocus();
                    if( field instanceof ListField ) {
                        switch( swipeDirection ) {
                            case TouchGesture.SWIPE_EAST:
                                doneButton.setFocus();
                                return true;
                        }
                    } else if ( field instanceof ButtonField ) {
                        switch( swipeDirection ) {
                            case TouchGesture.SWIPE_NORTH:
                            case TouchGesture.SWIPE_WEST:
                                listChoices.setFocus();
                                listChoices.setSelectedIndex( 1 );// Set to previously selected index );
                                return true;
                        }
                    }
                } 
        }
        return super.touchEvent(message);
    }    

    /* @Override */ 
    protected boolean keyChar(char c, int status, int time) {
        switch ( c ) {
            case Characters.ENTER:
                return true;
            case  Characters.ESCAPE:
                close();
                return true;
            default:
                updateCurrentSelection( c );
                break;
        }
        return super.keyChar( c, status, time );
    }
    
    /*
     * Store choice information.
     */
    private static final class ListItem {
        private final String _label;
        private boolean _selected;
        
        public ListItem(String label, boolean selected) {
            _label = label;
            _selected = selected;
        }
        
        /* @Override */ 
        public String toString() {
            return _label;
        }
        
        public void setSelected( boolean value ) {
            _selected = value;
        }

        public boolean isSelected() {
            return _selected;
        }
    }
    
    /*
     * Handle the popup dialog layout.
     */
    private static class PopupDelegate extends VerticalFieldManager {
        boolean _multiple;
        
        PopupDelegate( boolean allowMultiple ) {
            super( NO_VERTICAL_SCROLL | NO_VERTICAL_SCROLLBAR );
            _multiple = allowMultiple;
        }
        
        protected void sublayout( int maxWidth, int maxHeight ) {
            int yPosition = 0;
            int heightAvailable = maxHeight;
            Field field = getField( 0 );
            int numFields = getFieldCount();

            // Layout the vertical field manager that contains the listField
            layoutChild(field, maxWidth, heightAvailable);
            setPositionChild(field, 0, 0);
            
            boolean heightCheck; // Done button may not fit properly because of the font size and we need to take its height into account
            if ( _multiple ) {
                Field button = getField( numFields - 1 );
                layoutChild( button, maxWidth, heightAvailable);
                heightCheck = field.getHeight() < heightAvailable - 6 - button.getHeight(); //6 is for VSF height + SF height
            } else {
                heightCheck = field.getHeight() < heightAvailable;
            }
           
            if ( heightCheck ) { 
                // manager is taking less space then the total space available.
                // so call super which takes care of adjusting the popupscreen height
                super.sublayout( maxWidth, maxHeight );
            }    
            else {
                // start laying out fields in reverse order so that the remaining 
                // height can be given to the listField container.
                for(int index = numFields - 1; index >= 0; index--) {
                    field = getField( index );
                    if(field instanceof VerticalFieldManager) {
                        break;
                    } else {
                        layoutChild(field, maxWidth, heightAvailable);
                        yPosition += field.getHeight();
                        //Center the Done button
                        if(field.isStyle(Field.FIELD_HCENTER)) {
                            setPositionChild(field, (maxWidth - field.getWidth() + 1) >> 1, maxHeight - yPosition);
                        } else {
                            setPositionChild(field, 0, maxHeight - yPosition);
                        }
                        heightAvailable -= field.getHeight();
                    }
                }

                // Layout listField container with remaining height
                layoutChild(field, maxWidth, heightAvailable);
        
                setVirtualExtent( maxWidth, maxHeight );
                setExtent( maxWidth, maxHeight );
            } //else
        } //sublayout
    }
}