package syweb.halo.link.protect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jsoup.nodes.Document;
import org.pf4j.Extension;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.app.theme.ReactivePostContentHandler;
import org.springframework.stereotype.Component;
import org.jsoup.Jsoup;

import java.net.URISyntaxException;

@Slf4j
@RequiredArgsConstructor
@Component
@Extension
public class LinkProtectPostContentHandler implements ReactivePostContentHandler {

    private final ReactiveSettingFetcher reactiveSettingFetcher;

    @Override
    public Mono<PostContentContext> handle(PostContentContext postContent) {
        return reactiveSettingFetcher.fetch("basic", LinkProtectSetting.class)
            .defaultIfEmpty(new LinkProtectSetting())
            .flatMap(basicConfig -> {

                if (basicConfig.getEnableTimestampAntiLeech()) {
                    return reactiveSettingFetcher.fetch("timestamp_anti_leech", LinkProtectSetting.class)
                        .defaultIfEmpty(new LinkProtectSetting())
                        .flatMap(qiniuConfig -> {
                            qiniuConfig.setProtectedResourceScope(basicConfig.getProtectedResourceScope());
                            qiniuConfig.setResourceServiceAddress(basicConfig.getResourceServiceAddress());
                            String oldContent = postContent.getContent();
                            Document document = Jsoup.parse(oldContent);

                            document.select("img, video, audio, source").forEach(img -> {
                                String src = img.attr("src");
                                try {
                                    val newSrc = LinkProtectQiniuTimestamp.processLink(src, qiniuConfig);
                                    img.attr("src", newSrc);
                                    log.info("newSrc link: {}", newSrc);
                                    log.info("Protected link: {}", src);
                                    log.info("New link: {}", img.attr("src"));
                                } catch (URISyntaxException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                            postContent.setContent(document.outerHtml());
                            return Mono.just(postContent);
                        });
                }
                return Mono.just(postContent);
            });
    }
}