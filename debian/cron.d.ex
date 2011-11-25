#
# Regular cron jobs for the bytemine-manager package
#
0 4	* * *	root	[ -x /usr/bin/bytemine-manager_maintenance ] && /usr/bin/bytemine-manager_maintenance
