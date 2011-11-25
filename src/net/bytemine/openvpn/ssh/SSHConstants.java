/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.openvpn.ssh;


/**
 * Constants for ssh communication
 *
 * @author Daniel Rauer
 */
public class SSHConstants {

    public static final String SERVICE_CODE_VPNM = "VPNM";
    public static final String[] KNOWN_SERVICE_CODES = new String[]{SERVICE_CODE_VPNM};

    public static final int STATUS_CODE_UNDEFINED = -1;
    public static final int STATUS_CODE_READY = 1;
    public static final int STATUS_CODE_OK = 2;
    public static final int STATUS_CODE_FAIL = 3;
    public static final int STATUS_CODE_WAIT = 4;

    public static final int MAX_VERB_LEVEL = 7;
    
    public static final int MAX_LOGIN_ATTEMPTS = 3;

    public static final String KEYWORD_READY = "READY";
    public static final String KEYWORD_WAIT = "WAIT";
    public static final String KEYWORD_OK = "OK";
    public static final String KEYWORD_FAIL = "FAIL";
    public static final String KEYWORD_END = "END";
    public static final String KEYWORD_ROUTING_TABLE = "ROUTING TABLE";
    public static final String KEYWORD_UPDATED = "Updated,";
    public static final String KEYWORD_COMMON = "Common Name,Real Address,";
    public static final String KEYWORD_VIRTUAL_ADDRESS = "Virtual Address,Common Name,Real Address,Last Ref";
    public static final String KEYWORD_GLOBAL_STATS = "GLOBAL STATS";
    public static final String KEYWORD_CHANNELS = "CHANNELS:";
    public static final String KEYWORD_KILL_SUCCESS = "client(s) killed";
    public static final String KEYWORD_STATUS = "OpenVPN CLIENT LIST";
    public static final String KEYWORD_LOG = ">LOG:";
    public static final String KEYWORD_VERSION = "OpenVPN Version:";

    public static final String CHANNEL_PATTERN = "[0-9]+";
    public static final String KEYWORD_PATTERN_OPEN_FAILED = ".*open channel [0-9]+ failed.*";
    public static final String KEYWORD_PATTERN_OPEN_FAILED_TIME_OUT = ".*open [0-9]+.* Connection timed out.*";
    public static final String KEYWORD_PATTERN_KILL_FAILED = ".*common name .* not found.*";
    public static final String KEYWORD_PATTERN_INSTANCE_FAILED = ".*another instance of ut is already running, exiting.*";


    public static final String CHANNEL_COMMAND = "00";
    public static final String CHANNEL_DEBUG = "FF";

    public static final String CHANNEL_TYPE_SHELL = "shell";

    public static final String NEWLINE = "\n";

    public static final String COMMAND_STATUS = "status";
    public static final String COMMAND_OPEN = "open";
    public static final String COMMAND_CLOSE = "close";
    public static final String COMMAND_KILL = "kill";
    public static final String COMMAND_LOG_ON = "log on";
    public static final String COMMAND_LOG_OFF = "log off";
    public static final String COMMAND_VERSION = "version";

    public static final int PREFIX_UNDEFINED = 0;
    public static final int PREFIX_OUTPUT = 1;
    public static final int PREFIX_INPUT = 2;
    public static final int PREFIX_INPUT_NO_NEWLINE = 3;
    public static final int PREFIX_ERROR = 4;
    public static final int PREFIX_CLOSEDOWN = 5;

}
