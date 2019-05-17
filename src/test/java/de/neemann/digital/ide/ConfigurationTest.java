/*
 * Copyright (c) 2019 Helmut Neemann.
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digital.ide;

import de.neemann.digital.core.NodeException;
import de.neemann.digital.draw.elements.PinException;
import de.neemann.digital.draw.library.ElementNotFoundException;
import de.neemann.digital.integration.Resources;
import de.neemann.digital.integration.ToBreakRunner;
import junit.framework.TestCase;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class ConfigurationTest extends TestCase {

    public void testStart() throws IOException, ElementNotFoundException, PinException, NodeException {
        String xml = "<ide name=\"APIO\">\n" +
                "    <commands>\n" +
                "        <command name=\"build\" requires=\"verilog\" filter=\"false\">\n" +
                "            <arg>make</arg>\n" +
                "        </command>\n" +
                "        <command name=\"prog\" requires=\"verilog\" filter=\"true\">\n" +
                "            <arg>make</arg>\n" +
                "            <arg>&lt;?=dir?&gt;/&lt;?=shortname?&gt;.v</arg>\n" +
                "        </command>\n" +
                "    </commands>\n" +
                " </ide>\n";


        ToBreakRunner br = new ToBreakRunner(new File(Resources.getRoot(), "dig/hdl/negSimple.dig"));

        final TestIOInterface fileWriter = new TestIOInterface();
        Configuration c = Configuration.load(new ByteArrayInputStream(xml.getBytes()))
                .setFilenameProvider(() -> new File("z/test.dig"))
                .setCircuitProvider(br::getCircuit)
                .setLibraryProvider(br::getLibrary)
                .setIoInterface(fileWriter);
        ArrayList<Command> commands = c.getCommands();
        assertEquals(2, commands.size());

        c.executeCommand(commands.get(0));

        assertEquals(1, fileWriter.files.size());
        assertTrue(fileWriter.files.containsKey("z/test.v"));

        assertEquals(1, fileWriter.commands.size());
        assertEquals("z", fileWriter.commands.get(0).dir.getPath());
        assertEquals("[make]", Arrays.toString(fileWriter.commands.get(0).args));

        fileWriter.clear();
        c.executeCommand(commands.get(1));

        assertEquals(1, fileWriter.files.size());
        assertTrue(fileWriter.files.containsKey("z/test.v"));

        assertEquals(1, fileWriter.commands.size());
        assertEquals("z", fileWriter.commands.get(0).dir.getPath());
        assertEquals("[make, z/test.v]", Arrays.toString(fileWriter.commands.get(0).args));
    }

    public void testFileWriter() throws IOException, ElementNotFoundException, PinException, NodeException {
        String xml = "<ide name=\"APIO\">\n" +
                "    <commands>\n" +
                "        <command name=\"build\" requires=\"verilog\" filter=\"false\">\n" +
                "            <arg>make</arg>\n" +
                "        </command>\n" +
                "    </commands>\n" +
                "    <files>\n" +
                "        <file name=\"file1\" overwrite=\"true\" filter=\"false\">\n" +
                "            <content>deal with &lt;?=path?&gt;</content>\n" +
                "        </file>\n" +
                "        <file name=\"file2\" overwrite=\"true\" filter=\"true\">\n" +
                "            <content>deal with &lt;?=path?&gt;</content>\n" +
                "        </file>\n" +
                "        <file name=\"&lt;?=shortname?&gt;.z\" overwrite=\"true\" filter=\"false\">\n" +
                "            <content>test</content>\n" +
                "        </file>\n" +
                "    </files>\n" +
                " </ide>\n";


        ToBreakRunner br = new ToBreakRunner(new File(Resources.getRoot(), "dig/hdl/negSimple.dig"));

        final TestIOInterface fileWriter = new TestIOInterface();
        Configuration c = Configuration.load(new ByteArrayInputStream(xml.getBytes()))
                .setFilenameProvider(() -> new File("z/test.dig"))
                .setCircuitProvider(br::getCircuit)
                .setLibraryProvider(br::getLibrary)
                .setIoInterface(fileWriter);
        ArrayList<Command> commands = c.getCommands();
        assertEquals(1, commands.size());

        c.executeCommand(commands.get(0));

        assertEquals(4, fileWriter.files.size());
        assertEquals("deal with <?=path?>", fileWriter.files.get("z/file1").toString());
        assertEquals("deal with z/test.dig", fileWriter.files.get("z/file2").toString());
        assertEquals("test", fileWriter.files.get("z/test.z").toString());
    }


    static class TestIOInterface implements Configuration.IOInterface {
        private HashMap<String, ByteArrayOutputStream> files = new HashMap<>();
        private ArrayList<StartedCommand> commands = new ArrayList<>();

        @Override
        public OutputStream getOutputStream(File filename) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            files.put(filename.getPath(), baos);
            return baos;
        }

        @Override
        public void startProcess(Command command, File dir, boolean gui, String[] args) {
            commands.add(new StartedCommand(dir, args));
        }

        @Override
        public void showError(Command command, Exception e) {
            throw new RuntimeException(command.getName(), e);
        }

        private void clear() {
            files.clear();
            commands.clear();
        }

        public HashMap<String, ByteArrayOutputStream> getFiles() {
            return files;
        }
    }

    private static class StartedCommand {
        private final File dir;
        private final String[] args;

        private StartedCommand(File dir, String[] args) {
            this.dir = dir;
            this.args = args;
        }
    }
}