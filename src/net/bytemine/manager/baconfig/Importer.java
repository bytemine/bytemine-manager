/*************************************************************************
 * Written by / Copyright (C) 2009-2011 bytemine GmbH                     *
 * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
 *                                                                        *
 * http://www.bytemine.net/                                               *
 *************************************************************************/


package net.bytemine.manager.baconfig;

import java.io.FileReader;
import java.io.FileWriter;

import net.sourceforge.yamlbeans.YamlReader;
import net.sourceforge.yamlbeans.YamlWriter;


public class Importer {

    public static void main(String[] args) {
        try {
            YamlReader reader = new YamlReader(new FileReader("examples/ba-config.yml"));
            reader.getConfig().setClassTag("ruby/object:BytemineAppliance::Config::Object", Config.class);
            reader.getConfig().setClassTag("ruby/object:BytemineAppliance::Config::Network", Network.class);
            reader.getConfig().setClassTag("ruby/object:BytemineAppliance::Config::Network::Interface", Interface.class);
            reader.getConfig().setClassTag("ruby/object:BytemineAppliance::Config::Network::IPAddress", IPAddress.class);
            reader.getConfig().setClassTag("ruby/object:BytemineAppliance::Config::Network::PPPoEInterface", PPPoEInterface.class);
            reader.getConfig().setClassTag("ruby/object:BytemineAppliance::Config::Network::Route", Route.class);
            reader.getConfig().setClassTag("ruby/object:BytemineAppliance::Config::System", System.class);
            reader.getConfig().setClassTag("ruby/object:BytemineAppliance::Config::System::Startup", Startup.class);
            Config config = reader.read(Config.class);
            //java.lang.System.out.println(config.getNetwork().getInterfaces().get(0).getLladdr());
            //java.lang.System.out.println(config.getNetwork().getTun_count());
            //java.lang.System.out.println(config.getSystem().getNtp_server().get(0));
            
            YamlWriter writer = new YamlWriter(new FileWriter("examples/ba-config-out.yml"));
            // writes also false values
            writer.getConfig().writeConfig.setWriteDefaultValues(true);
            // writes root tag with prefix '--- '
            writer.getConfig().writeConfig.setExplicitFirstDocument(true);
            // writes every unknown classname out
            writer.getConfig().writeConfig.setAlwaysWriteClassname(true);
            writer.getConfig().setClassTag("ruby/object:BytemineAppliance::Config::Object", Config.class);
            writer.getConfig().setClassTag("ruby/object:BytemineAppliance::Config::Network", Network.class);
            writer.getConfig().setClassTag("ruby/object:BytemineAppliance::Config::Network::Interface", Interface.class);
            writer.getConfig().setClassTag("ruby/object:BytemineAppliance::Config::Network::IPAddress", IPAddress.class);
            writer.getConfig().setClassTag("ruby/object:BytemineAppliance::Config::Network::PPPoEInterface", PPPoEInterface.class);
            writer.getConfig().setClassTag("ruby/object:BytemineAppliance::Config::Network::Route", Route.class);
            writer.getConfig().setClassTag("ruby/object:BytemineAppliance::Config::System", System.class);
            writer.getConfig().setClassTag("ruby/object:BytemineAppliance::Config::System::Startup", Startup.class);
            writer.write(config); 
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
