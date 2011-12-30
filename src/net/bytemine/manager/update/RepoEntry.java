/*************************************************************************
 * Written by / Copyright (C) 2009-2012 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


package net.bytemine.manager.update;

import java.util.HashMap;

import net.bytemine.utility.DebugPrinter;
import net.bytemine.utility.StringUtils;


/**
 * Represents an entry from the repo.yml
 *
 * @author Daniel Rauer
 */
public class RepoEntry implements Comparable<RepoEntry> {

    private String version;
    private String filename;
    private String checksum;
    private String[] jars;
    private long size;
    private long timestamp;
    private String changelog;


    public RepoEntry(HashMap<String, Object> map) {
        DebugPrinter.printMap(map);
        if (map != null) {
            version = map.get("version").toString();
            filename = map.get("filename").toString();
            checksum = map.get("checksum").toString();
            if (map.containsKey("jars"))
                jars = StringUtils.tokenize(map.get("jars").toString(), "");
            size = Long.parseLong(map.get("size").toString());
            timestamp = Long.parseLong(map.get("timestamp").toString());
            if (map.containsKey("changelog"))
                changelog = map.get("changelog").toString();
        }
    }


    public String getVersion() {
        return version;
    }

    public String getFilename() {
        return filename;
    }

    public String[] getJars() {
        return jars;
    }

    public long getSize() {
        return size;
    }

    public String getChecksum() {
        return checksum;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getChangelog() {
        return changelog;
    }

    public void setChangelog(String changelog) {
        this.changelog = changelog;
    }


    public int compareTo(RepoEntry re) {
        String[] tokens = StringUtils.tokenize(re.getVersion());
        if (isNewerThanCurrentVersion(tokens))
            return -1;
        else
            return 1;
    }


    /**
     * Decides if the version from this entry is newer than the apps current version
     *
     * @param currentTokens String[] with the tokens from the current version
     * @return true, if the new version is newer than the current
     */
    public boolean isNewerThanCurrentVersion(String[] currentTokens) {
        int result = 0;
        String[] newTokens = StringUtils.tokenize(version);
        for (int i = 0; i < newTokens.length; i++) {
            // the new version string is longer than the current
            if (currentTokens.length < i + 1) {
                // if last comparison ended for the newer version or equality, it is newer
                // e.g.: 1.0 to 1.0.4 => 1.0 is equal to 1.0, but the newer version is longer
                if (result >= 0)
                    return true;
                else
                    return false;
            }

            String string = newTokens[i];
            int newNr = Integer.parseInt(string);

            int currentNr = Integer.parseInt(currentTokens[i]);
            if (newNr > currentNr)
                result = 1;
            else if (newNr == currentNr)
                result = 0;
            else
                result = -1;
        }
        return result>0;
    }

}
