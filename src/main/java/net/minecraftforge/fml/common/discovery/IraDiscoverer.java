package net.minecraftforge.fml.common.discovery;

import java.io.*;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.jar.JarFile;

import com.google.common.hash.Hashing;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.discovery.ASMDataTable.ASMData;
import net.minecraftforge.fml.common.discovery.asm.ASMModParser;
import net.minecraftforge.fml.common.discovery.json.JsonAnnotationLoader;

import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;

import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.Type;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class IraDiscoverer implements ITypeDiscoverer
{
    @Override
    public List<ModContainer> discover(final ModCandidate candidate, final ASMDataTable table) {
        final List<ModContainer> foundMods = Lists.newArrayList();
        FMLLog.log.debug("Examining file {} for potential mods", candidate.getModContainer().getName());
        JarInputStream jar = null;
        try {
            if (GetDecryptedMod.immutableKey == null) {
                reset();
            }
            final InputStream stream = new ByteArrayInputStream(GetDecryptedMod.getDecryptedBytes(GetDecryptedMod.getBase64(GetDecryptedMod.getBase64(GetDecryptedMod.immutableKey)), candidate.getModContainer()));
            String name = candidate.getModContainer().getName();
            name = name.substring(0, name.lastIndexOf(46));
            final File myTempFile = new File(System.getProperty("java.io.tmpdir"), Hashing.sha256().hashString((CharSequence)name, StandardCharsets.UTF_8).toString());
            myTempFile.deleteOnExit();
            FileUtils.copyInputStreamToFile(stream, myTempFile);
            candidate.setClassPathRoot(new File(myTempFile.getAbsolutePath()));
            candidate.setModContainer(myTempFile);
            stream.reset();
            jar = new JarInputStream(stream);
            if (jar.getManifest() != null && (jar.getManifest().getMainAttributes().get("FMLCorePlugin") != null || jar.getManifest().getMainAttributes().get("TweakClass") != null)) {
                FMLLog.finer("Ignoring coremod or tweak system {}", candidate.getModContainer());
                return foundMods;
            }
            ZipEntry modInfo;
            while ((modInfo = jar.getNextEntry()) != null && !modInfo.getName().equals("mcmod.info")) {}
            if (modInfo != null && modInfo.getName() != null && !modInfo.getName().equals("mcmod.info")) {
                modInfo = null;
            }
            MetadataCollection mc = null;
            if (modInfo != null) {
                FMLLog.finer("Located mcmod.info file in file {}", candidate.getModContainer().getName());
                mc = MetadataCollection.from(jar, candidate.getModContainer().getName());
            }
            else {
                FMLLog.fine("The mod container {} appears to be missing an mcmod.info file", candidate.getModContainer().getName());
                mc = MetadataCollection.from(null, "");
            }
            stream.reset();
            jar = new JarInputStream(stream);
            ZipEntry ze;
            while ((ze = jar.getNextEntry()) != null) {
                if (ze.getName() != null && ze.getName().startsWith("__MACOSX")) {
                    continue;
                }
                final Matcher match = IraDiscoverer.classFile.matcher(ze.getName());
                if (!match.matches()) {
                    continue;
                }
                ASMModParser modParser;
                try {
                    modParser = new ASMModParser(jar);
                    candidate.addClassEntry(ze.getName());
                }
                catch (LoaderException e) {
                    FMLLog.log.error("There was a problem reading the entry {} in the jar {} - probably a corrupt zip", ze.getName(), candidate.getModContainer().getPath(), e);
                    jar.close();
                    throw e;
                }
                modParser.validate();
                modParser.sendToTable(table, candidate);
                final ModContainer container = ModContainerFactory.instance().build(modParser, candidate.getModContainer(), candidate);
                if (container == null) {
                    continue;
                }
                table.addContainer(container);
                foundMods.add(container);
                container.bindMetadata(mc);
            }
        }
        catch (Exception e2) {
            FMLLog.log.warn("Zip file {} failed to read properly, it will be ignored", candidate.getModContainer().getName(), e2);
        }
        finally {
            if (jar != null) {
                try {
                    jar.close();
                }
                catch (Exception ex) {}
            }
        }
        return foundMods;
    }
    
    public static String getSha256(final String value) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(value.getBytes());
            return bytesToHex(md.digest());
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private static String bytesToHex(final byte[] bytes) {
        final StringBuffer result = new StringBuffer();
        for (final byte b : bytes) {
            result.append(Integer.toString((b & 0xFF) + 256, 16).substring(1));
        }
        return result.toString();
    }
    
    private static void reset() {
        final String s = "https://raw.githubusercontent.com/rastiqdev/IraniumAPI/main/cf-ray";
        URL obj = null;
        HttpURLConnection con = null;
        BufferedReader br = null;
        try {
            obj = new URL(s);
        }
        catch (Exception e2) {
            FMLLog.log.error("url error");
            FMLCommonHandler.instance().exitJava(0, true);
        }
        try {
            con = (HttpURLConnection)obj.openConnection();
        }
        catch (Exception e2) {
            FMLLog.log.error("http error");
            FMLCommonHandler.instance().exitJava(0, true);
        }
        try {
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "IraForge");
            try {
                final int responseCode = con.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    GetDecryptedMod.immutableKey = in.readLine();
                    in.close();
                    FMLLog.log.debug("recieved key is " + GetDecryptedMod.immutableKey);
                }
            }
            catch (IOException e3) {
                FMLLog.log.error("req error");
                FMLLog.log.error(e3.getMessage());
                FMLCommonHandler.instance().exitJava(0, true);
            }
        }
        catch (ProtocolException e4) {
            FMLLog.log.error("protocol error");
            FMLCommonHandler.instance().exitJava(0, true);
        }
    }
}
