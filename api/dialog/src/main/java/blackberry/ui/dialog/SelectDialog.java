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
import net.rim.device.api.ui.component.ListField;
import net.rim.device.api.ui.component.ListFieldCallback;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.container.PopupScreen;
import net.rim.device.api.ui.container.VerticalFieldManager;
import net.rim.device.api.ui.input.InputSettings;
import net.rim.device.api.ui.input.NavigationDeviceSettings;

/**
 * Implementation of selection dialog
 * 
 * @author jachoi
 * 
 */
public class SelectDialog extends PopupScreen implements FieldChangeListener {

    private SelectDialog _thisDialog;
    private ButtonField _doneButton;
    private VerticalFieldManager _vfm;
    private SelectListField _list;

    private DialogListener _closeListener;

    private ListItem[] _listItems;
    private int[] _response = null;

    private int _choiceLength;
    private boolean _allowMultiple;
    private int _selectedIndex;

    public SelectDialog( boolean allowMultiple, String[] labels, boolean[] enableds, boolean[] selecteds ) {
        super( new PopupDelegate( allowMultiple ) );
        _choiceLength = labels.length;
        _thisDialog = this;
        _allowMultiple = allowMultiple;
        _selectedIndex = -1;

        _listItems = new ListItem[ _choiceLength ];
        for( int index = 0; index < _choiceLength; index++ ) {
            if( _selectedIndex == -1 && selecteds[ index ] && enableds[ index ] ) {
                _selectedIndex = index;
            }
            _listItems[ index ] = new ListItem( labels[ index ], enableds[ index ], selecteds[ index ] );
        }

        _list = new SelectListField();
        _list.setChangeListener( this );
        _vfm = new VerticalFieldManager( NO_HORIZONTAL_SCROLL | NO_HORIZONTAL_SCROLLBAR | VERTICAL_SCROLL | VERTICAL_SCROLLBAR );
        _vfm.add( _list );
        add( _vfm );

        if( _allowMultiple ) {
            _doneButton = new ButtonField( "DONE", Field.FIELD_HCENTER );
            _doneButton.setChangeListener( this );

            add( new SeparatorField() );
            add( _doneButton );
        }

        // Enable swipe with the track-pad.
        final InputSettings inputSettings = NavigationDeviceSettings.createEmptySet();
        inputSettings.set( NavigationDeviceSettings.DETECT_SWIPE, 1 );
        addInputSettings( inputSettings );
    }

    public void setDialogListener( DialogListener dialogListener ) {
        _closeListener = dialogListener;
    }

    public void display() {
        new Thread( new Runnable() {
            public void run() {
                UiApplication.getUiApplication().invokeLater( new Runnable() {
                    public void run() {
                        UiApplication.getUiApplication().pushScreen( _thisDialog );
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
                        UiApplication.getUiApplication().popScreen( _thisDialog );
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
        if( field == _doneButton ) {
            int counter = 0;

            for( int index = 0; index < _listItems.length; index++ ) {
                if( _listItems[ index ].isSelected() ) {
                    counter++;
                }
            }
            int[] response = new int[ counter ];
            int responseIndex = 0;
            for( int index = 0; index < _listItems.length; index++ ) {
                if( _listItems[ index ].isSelected() ) {
                    response[ responseIndex ] = index;
                    responseIndex++;
                }
            }
            _response = response;
        } else {
            _response = new int[] { _selectedIndex };
        }
    }

    private void updateCurrentSelection( char keyChar ) {
        // Ensure we were passed a valid key to locate.
        if( keyChar == '\u0000' ) {
            return;
        }
    }

    /* @Override */
    protected boolean touchEvent( TouchEvent message ) {
        switch( message.getEvent() ) {
            case TouchEvent.GESTURE:
                if( _allowMultiple && message.getGesture().getEvent() == TouchGesture.NAVIGATION_SWIPE ) {
                    int swipeDirection = message.getGesture().getSwipeDirection();
                    Field field = getLeafFieldWithFocus();
                    if( field instanceof ListField ) {
                        switch( swipeDirection ) {
                            case TouchGesture.SWIPE_EAST:
                                _doneButton.setFocus();
                                return true;
                        }
                    } else if( field instanceof ButtonField ) {
                        switch( swipeDirection ) {
                            case TouchGesture.SWIPE_NORTH:
                            case TouchGesture.SWIPE_WEST:
                                _list.setFocus();
                                _list.setSelectedIndex( _list._previousSelected );
                                return true;
                        }
                    }
                }
        }
        return super.touchEvent( message );
    }

    /* @Override */
    protected boolean keyChar( char c, int status, int time ) {
        switch( c ) {
            case Characters.ENTER:
                // _list.invokeAction(Field.ACTION_INVOKE);
                return true;
            case Characters.ESCAPE:
                close();
                return true;
            default:
                updateCurrentSelection( c );
                break;
        }
        return super.keyChar( c, status, time );
    }

    private final class SelectListField extends ListField implements ListFieldCallback {

        public int _previousSelected = -1;

        SelectListField() {
            setCallback( this );
            setSize( _choiceLength );
            setSelectedIndex( _selectedIndex );
        }

        protected void onUnfocus() {
            _previousSelected = this.getSelectedIndex();
            super.onUnfocus();
        }

        protected boolean invokeAction( int action ) {
            if( action == Field.ACTION_INVOKE ) {
                int selectedIndex = getSelectedIndex();
                ListItem listItem = (ListItem) get( this, selectedIndex );

                if( !listItem.isEnabled() ) {
                    return true;
                }

                if( !_allowMultiple ) {
                    if( _selectedIndex != -1 ) {
                        _listItems[ _selectedIndex ].setSelected( false );
                    }
                    listItem.setSelected( true );
                    _selectedIndex = selectedIndex;
                    fieldChanged( null, -1 );
                } else {
                    listItem.setSelected( !listItem.isSelected() );
                    invalidate();
                }
                return true;
            }
            return false;
        }

        public void drawListRow( ListField listField, Graphics graphics, int index, int y, int w ) {
            Object obj = get( listField, index );
            if( obj instanceof ListItem ) {
                paintListItem( (ListItem) obj, listField, graphics, index, y, w );
            }
        }

        private void paintListItem( ListItem listItem, ListField listField, Graphics graphics, int index, int y, int width ) {
            if( !listItem.isEnabled() ) {
                graphics.setColor( Color.GRAY );
            }

            if( _allowMultiple ) {
                if( listItem.isSelected() ) {
                    graphics.drawText( '\u2611' + "   " + listItem.toString(), 0, y, 0, width );
                } else {
                    graphics.drawText( '\u2610' + "   " + listItem.toString(), 0, y, 0, width );
                }
            } else {
                if( listItem.isSelected() ) {
                    graphics.drawText( '\u2714' + "   " + listItem.toString(), 0, y, 0, width );
                } else {
                    graphics.drawText( "      " + listItem.toString(), 0, y, 0, width );
                }
            }
        }

        public Object get( ListField list, int index ) {
            return _listItems[ index ];
        }

        public int getPreferredWidth( ListField arg0 ) {
            return Display.getWidth();
        }

        public int indexOfList( ListField arg0, String arg1, int arg2 ) {
            return -1;
        }
    }

    /*
     * Store choice information.
     */
    private static final class ListItem {
        private final String _label;
        private boolean _selected;
        private boolean _enabled;

        public ListItem( String label, boolean enabled, boolean selected ) {
            _label = label;
            _selected = selected;
            _enabled = enabled;
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

        public boolean isEnabled() {
            return _enabled;
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
            layoutChild( field, maxWidth, heightAvailable );
            setPositionChild( field, 0, 0 );

            boolean heightCheck; // Done button may not fit properly because of the font size and we need to take its height into
                                 // account
            if( _multiple ) {
                Field button = getField( numFields - 1 );
                layoutChild( button, maxWidth, heightAvailable );
                heightCheck = field.getHeight() < heightAvailable - 6 - button.getHeight(); // 6 is for VSF height + SF height
            } else {
                heightCheck = field.getHeight() < heightAvailable;
            }

            if( heightCheck ) {
                // manager is taking less space then the total space available.
                // so call super which takes care of adjusting the popupscreen height
                super.sublayout( maxWidth, maxHeight );
            } else {
                // start laying out fields in reverse order so that the remaining
                // height can be given to the listField container.
                for( int index = numFields - 1; index >= 0; index-- ) {
                    field = getField( index );
                    if( field instanceof VerticalFieldManager ) {
                        break;
                    } else {
                        layoutChild( field, maxWidth, heightAvailable );
                        yPosition += field.getHeight();
                        // Center the Done button
                        if( field.isStyle( Field.FIELD_HCENTER ) ) {
                            setPositionChild( field, ( maxWidth - field.getWidth() + 1 ) >> 1, maxHeight - yPosition );
                        } else {
                            setPositionChild( field, 0, maxHeight - yPosition );
                        }
                        heightAvailable -= field.getHeight();
                    }
                }

                // Layout listField container with remaining height
                layoutChild( field, maxWidth, heightAvailable );

                setVirtualExtent( maxWidth, maxHeight );
                setExtent( maxWidth, maxHeight );
            } // else
        } // sublayout
    }
}