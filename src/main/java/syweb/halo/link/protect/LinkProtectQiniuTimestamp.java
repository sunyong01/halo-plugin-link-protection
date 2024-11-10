package syweb.halo.link.protect;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.util.DigestUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Slf4j
public class LinkProtectQiniuTimestamp {

    public static String processLink(String RawLink, LinkProtectSetting linkProtectSetting)
        throws URISyntaxException {
        // 如果链接为空，直接返回
        // val TestURL = "http://xxx.yyy.com/DIR1/dir2/vodfile.mp4?v=1.1";
        // 获取URL的Path部分
        URI uri = new URI(RawLink);
        String path = uri.getPath();
        if (!linkProtectSetting.getResourceServiceAddress().isEmpty()) {
            // 如果链接中包含资源服务地址，就去除这个地址
            if (path.startsWith(linkProtectSetting.getResourceServiceAddress())) {
                path = path.substring(linkProtectSetting.getResourceServiceAddress().length());
            }
        }

        List supportedTypes = linkProtectSetting.getType();
        // 如果不是支持的资源类型，直接返回
        String extension = getFileExtension(path);
        if (!supportedTypes.contains(extension)) {
            log.info("Unsupported file type: {}", extension);
            return RawLink;
        }

        String expirationTime = buildExpirationTime(
            Integer.parseInt(linkProtectSetting.getExpirationTimeMinutes()));
        String encodePath = urlEncode(path);
        String sign = makeSign(linkProtectSetting.getAuthKeys(), encodePath, expirationTime);
        // 构建新的URL
        //如果RawLink已经包含参数，就使用&连接新参数，否则使用?连接新参数
        if (RawLink.contains("?")) {
            return RawLink + "&" + linkProtectSetting.getParamFieldName() + "=" + sign + "&t="
                + expirationTime;
        }
        return RawLink + "?" + linkProtectSetting.getParamFieldName() + "=" + sign + "&t="
            + expirationTime;
    }

    /**
     * 构建T参数，表示URL的过期时间，以Unix时间戳的16进制小写形式表示
     *
     * @param minutes 倒计时分钟数
     * @return 过期时间的16进制小写形式
     */
    public static String buildExpirationTime(int minutes) {
        // 获取当前时间的Unix时间戳
        long currentTime = Instant.now().getEpochSecond();
        // 将倒计时分钟数转换为秒数，并加到当前时间戳上
        long expirationTime = currentTime + (minutes * 60L);
        // 将结果转换为16进制小写形式
        return Long.toHexString(expirationTime);
    }

    private static boolean isAbsoluteUrl(String url) {
        // 检查URL是否以协议（如http://或https://）开头
        return url.startsWith("http://") || url.startsWith("https://");
    }

    /**
     * 对字符串进行URL编码，保留斜杠不被编码
     *
     * @param s 输入的字符串
     * @return 编码后的字符串
     */
    public static String urlEncode(String s) {
        // 使用UTF-8编码对字符串进行URL编码
        String encoded = URLEncoder.encode(s, StandardCharsets.UTF_8);
        // 将编码后的字符串中的%2F替换回斜杠/
        encoded = encoded.replace("%2F", "/");
        return encoded;
    }

    public static String makeSign(String secretKey, String encodePath, String expirationTime) {
        // 构建签名文本
        String signText = secretKey + encodePath + expirationTime;
        // 使用MD5算法对签名文本进行加密
        String sign = DigestUtils.md5DigestAsHex(signText.getBytes());
        return sign.toLowerCase();
    }


    /**
     * 获取URI中的文件后缀名，如果没有后缀名返回空字符串
     *
     * @param uriString 输入的URI字符串
     * @return 文件后缀名，如果没有后缀名返回空字符串
     */
    public static String getFileExtension(String uriString) {
        try {
            URI uri = new URI(uriString);
            String path = uri.getPath();

            if (path != null && path.contains(".")) {
                int lastDotIndex = path.lastIndexOf('.');
                if (lastDotIndex != -1 && lastDotIndex < path.length() - 1) {
                    return path.substring(lastDotIndex);
                }
            }
        } catch (URISyntaxException e) {
            log.error("Failed to parse URI: {}", uriString, e);
        }

        return "";
    }
}
