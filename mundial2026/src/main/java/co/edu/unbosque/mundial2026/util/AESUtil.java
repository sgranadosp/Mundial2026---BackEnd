/**
 * Paquete que contiene las clases de utilidad de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.util;

import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Clase de utilidad para cifrado AES y hashing criptográfico en la plataforma
 * Mundial 2026 Hub.
 * <p>
 * Utiliza AES en modo GCM (Galois/Counter Mode) con autenticación integrada,
 * que provee confidencialidad e integridad del dato en una sola operación.
 * Los campos sensibles del usuario (nombre, username, email, código de
 * verificación) se encriptan antes de persistir en base de datos y se
 * desencriptan al leer, siguiendo el mismo patrón del proyecto VirusDetected.
 * </p>
 * <p>
 * Los métodos de conveniencia {@link #encrypt(String)} y {@link #decrypt(String)}
 * usan la clave e IV configurados internamente. La clave y el IV deben moverse
 * a variables de entorno o a {@code application.properties} antes del despliegue
 * en producción.
 * </p>
 */
public class AESUtil {

    /**
     * Nombre del algoritmo de cifrado.
     */
    private static final String ALGORITMO = "AES";

    /**
     * Modo de cifrado: AES con GCM y sin padding.
     * GCM es autenticado: detecta modificaciones al cifrado sin necesidad de HMAC externo.
     */
    private static final String TIPO_CIFRADO = "AES/GCM/NoPadding";

    /**
     * Clave de cifrado AES de 16 bytes (128 bits).
     * Debe externalizarse a una variable de entorno en producción.
     */
    private static final String KEY_DEFAULT = "mundial2026hubkk";

    /**
     * Vector de inicialización (IV) de 16 bytes para GCM.
     * Debe externalizarse a una variable de entorno en producción.
     */
    private static final String IV_DEFAULT = "mundial2026ivhub";

    // =========================================================================
    // Métodos de conveniencia (clave e IV predeterminados)
    // =========================================================================

    /**
     * Encripta un texto plano usando la clave e IV predeterminados del sistema.
     * Es el método principal usado por los servicios de la aplicación.
     *
     * @param plainText El texto plano a encriptar.
     * @return El texto encriptado en formato Base64, o {@code null} si ocurre
     *         un error.
     */
    public static String encrypt(String plainText) {
        if (plainText == null) return null;
        return encrypt(KEY_DEFAULT, IV_DEFAULT, plainText);
    }

    /**
     * Desencripta un texto cifrado en Base64 usando la clave e IV predeterminados.
     * Es el método principal usado por los servicios de la aplicación.
     *
     * @param encrypted El texto cifrado en formato Base64.
     * @return El texto plano desencriptado, o {@code null} si ocurre un error.
     */
    public static String decrypt(String encrypted) {
        if (encrypted == null) return null;
        return decrypt(KEY_DEFAULT, IV_DEFAULT, encrypted);
    }

    // =========================================================================
    // Métodos de cifrado con clave e IV explícitos
    // =========================================================================

    /**
     * Encripta un texto plano con la clave y el vector de inicialización
     * proporcionados usando AES/GCM/NoPadding.
     *
     * @param llave  La clave de cifrado AES de exactamente 16 bytes.
     * @param iv     El vector de inicialización de exactamente 16 bytes.
     * @param texto  El texto plano a encriptar.
     * @return El texto cifrado en formato Base64, o {@code null} si ocurre
     *         un error durante el proceso de cifrado.
     */
    public static String encrypt(String llave, String iv, String texto) {
        try {
            Cipher cipher = Cipher.getInstance(TIPO_CIFRADO);
            SecretKeySpec secretKeySpec = new SecretKeySpec(llave.getBytes(), ALGORITMO);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv.getBytes());
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmParameterSpec);
            byte[] encrypted = cipher.doFinal(texto.getBytes());
            return new String(encodeBase64(encrypted));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidKeyException | InvalidAlgorithmParameterException
                | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Desencripta un texto cifrado en Base64 con la clave y el vector de
     * inicialización proporcionados usando AES/GCM/NoPadding.
     *
     * @param llave     La clave de descifrado AES de exactamente 16 bytes.
     * @param iv        El vector de inicialización de exactamente 16 bytes.
     * @param encrypted El texto cifrado en formato Base64.
     * @return El texto plano desencriptado, o {@code null} si ocurre un error
     *         durante el proceso de descifrado.
     */
    public static String decrypt(String llave, String iv, String encrypted) {
        try {
            Cipher cipher = Cipher.getInstance(TIPO_CIFRADO);
            SecretKeySpec secretKeySpec = new SecretKeySpec(llave.getBytes(), ALGORITMO);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv.getBytes());
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec);
            byte[] enc = decodeBase64(encrypted);
            byte[] decrypted = cipher.doFinal(enc);
            return new String(decrypted);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidKeyException | InvalidAlgorithmParameterException
                | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            return null;
        }
    }

    // =========================================================================
    // Métodos de hashing (sin retorno reversible)
    // =========================================================================

    /**
     * Genera el hash MD5 de un contenido dado.
     * No es reversible; se usa solo para comparaciones.
     *
     * @param content El contenido a hashear.
     * @return El hash MD5 en formato hexadecimal.
     */
    public static String hashingToMD5(String content) {
        return DigestUtils.md5Hex(content);
    }

    /**
     * Genera el hash SHA-1 de un contenido dado.
     *
     * @param content El contenido a hashear.
     * @return El hash SHA-1 en formato hexadecimal.
     */
    public static String hashingToSHA1(String content) {
        return DigestUtils.sha1Hex(content);
    }

    /**
     * Genera el hash SHA-256 de un contenido dado.
     *
     * @param content El contenido a hashear.
     * @return El hash SHA-256 en formato hexadecimal.
     */
    public static String hashingToSHA256(String content) {
        return DigestUtils.sha256Hex(content);
    }

    /**
     * Genera el hash SHA-384 de un contenido dado.
     *
     * @param content El contenido a hashear.
     * @return El hash SHA-384 en formato hexadecimal.
     */
    public static String hashingToSHA384(String content) {
        return DigestUtils.sha384Hex(content);
    }

    /**
     * Genera el hash SHA-512 de un contenido dado.
     *
     * @param content El contenido a hashear.
     * @return El hash SHA-512 en formato hexadecimal.
     */
    public static String hashingToSHA512(String content) {
        return DigestUtils.sha512Hex(content);
    }
}