/**
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.environment.se.discovery.url;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.jboss.logging.Logger;

/**
 * This class provides file-system orientated scanning
 *
 * @author Pete Muir
 * @author Marko Luksa
 */
public class FileSystemURLHandler {

    private static final Logger log = Logger.getLogger(FileSystemURLHandler.class);

    private static final String CLASS_FILE_EXTENSION = ".class";
    private static final String BEANS_XML = "beans.xml";

    private List<String> discoveredClasses = new ArrayList<String>();
    private List<URL> discoveredBeansXmlUrls = new ArrayList<URL>();

    public void handle(String urlPath) {
        try {
            log.tracev("scanning: {0}", urlPath);

            File file = new File(urlPath);
            if (file.isDirectory()) {
                handleDirectory(file, null);
            } else {
                handleArchiveByFile(file);
            }
        } catch (IOException ioe) {
            log.warn("could not read entries", ioe);
        }
    }

    private void handleArchiveByFile(File file) throws IOException {
        try {
            log.tracev("archive: {0}", file);

            String archiveUrl = "jar:" + file.toURI().toURL().toExternalForm() + "!/";
            ZipFile zip = new ZipFile(file);
            Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                addToDiscovered(name, new URL(archiveUrl + name));
            }
            zip.close();
        } catch (ZipException e) {
            throw new RuntimeException("Error handling file " + file, e);
        }
    }

    private void handleDirectory(File dir, String path) {
        log.tracev("handling directory: {0}", dir);

        File[] files = dir.listFiles();
        assert files != null;
        for (File child : files) {
            String newPath = (path == null) ? child.getName() : (path + '/' + child.getName());

            if (child.isDirectory()) {
                handleDirectory(child, newPath);
            } else {
                try {
                    addToDiscovered(newPath, child.toURI().toURL());
                } catch (MalformedURLException e) {
                    log.errorv("Error loading file {0}", newPath);
                }
            }
        }
    }

    protected void addToDiscovered(String name, URL url) {
        if (name.endsWith(CLASS_FILE_EXTENSION)) {
            discoveredClasses.add(filenameToClassname(name));
        } else if (name.endsWith(BEANS_XML)) {
            discoveredBeansXmlUrls.add(url);
        }
    }

    /**
     * Convert a path to a class file to a class name
     */
    public static String filenameToClassname(String filename) {
        return filename.substring(0, filename.lastIndexOf(CLASS_FILE_EXTENSION)).replace('/', '.').replace('\\', '.');
    }

    public List<String> getDiscoveredClasses() {
        return discoveredClasses;
    }

    public List<URL> getDiscoveredBeansXmlUrls() {
        return discoveredBeansXmlUrls;
    }
}
