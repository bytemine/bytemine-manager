/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


package net.bytemine.manager.exception;


/**
 * handles validation exceptions
 * Extends VisualException
 *
 * @author Daniel Rauer
 */
public class ValidationException extends Exception {

    private static final long serialVersionUID = 1L;

    private String title = null;
    private int code = 0;

    /**
     * Creates a new ValidationException
     * @param exc The exception
     */
    public ValidationException(Throwable exc) {
        super(exc);
    }

    /**
     * Creates a new ValidationException
     * @param msg The message to display
     * @param exc The exception
     */
    public ValidationException(String msg, Throwable exc) {
        super(msg, exc);
    }

    /**
     * Creates a new ValidationException
     * @param msg The message to display
     */
    public ValidationException(String msg) {
        super(msg);
    }

    /**
     * Creates a new ValidationException
     * @param msg The message to display
     * @param title A title
     */
    public ValidationException(String msg, String title) {
        super(msg);
        this.title = title;
    }
    
    /**
     * Creates a new ValidationException
     * @param msg The message to display
     * @param title A title
     * @param code A code, representing the field in which the error occurred
     */
    public ValidationException(String msg, String title, int code) {
        super(msg);
        this.title = title;
        this.code = code;
    }

    public ValidationException() {
        super();
    }


    public String getTitle() {
        return this.title;
    }
    
    public int getCode() {
        return this.code;
    }

}
