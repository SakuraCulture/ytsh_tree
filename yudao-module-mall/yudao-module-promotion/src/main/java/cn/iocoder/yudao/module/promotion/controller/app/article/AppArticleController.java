package cn.iocoder.yudao.module.promotion.controller.app.article;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.promotion.controller.app.article.vo.article.AppArticlePageReqVO;
import cn.iocoder.yudao.module.promotion.controller.app.article.vo.article.AppArticleRespVO;
import cn.iocoder.yudao.module.promotion.convert.article.ArticleConvert;
import cn.iocoder.yudao.module.promotion.dal.dataobject.article.ArticleDO;
import cn.iocoder.yudao.module.promotion.service.article.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "鐢ㄦ埛 APP - 鏂囩珷")
@RestController
@RequestMapping("/promotion/article")
@Validated
public class AppArticleController {

    @Resource
    private ArticleService articleService;

    @RequestMapping("/list")
    @Operation(summary = "鑾峰緱鏂囩珷璇︽儏鍒楄〃")
    @Parameters({
            @Parameter(name = "recommendHot", description = "鏄惁鐑棬", example = "false"), // 鍦烘櫙涓€锛氭煡鐪嬫寚瀹氱殑鏂囩珷
            @Parameter(name = "recommendBanner", description = "鏄惁杞挱鍥?, example = "false") // 鍦烘櫙浜岋細鏌ョ湅鎸囧畾鐨勬枃绔?    })
    public CommonResult<List<AppArticleRespVO>> getArticleList(
            @RequestParam(value = "recommendHot", required = false) Boolean recommendHot,
            @RequestParam(value = "recommendBanner", required = false) Boolean recommendBanner) {
        return success(ArticleConvert.INSTANCE.convertList03(
                articleService.getArticleCategoryListByRecommend(recommendHot, recommendBanner)));
    }

    @RequestMapping("/page")
    @Operation(summary = "鑾峰緱鏂囩珷璇︽儏鍒嗛〉")
    public CommonResult<PageResult<AppArticleRespVO>> getArticlePage(AppArticlePageReqVO pageReqVO) {
        return success(ArticleConvert.INSTANCE.convertPage02(articleService.getArticlePage(pageReqVO)));
    }

    @RequestMapping("/get")
    @Operation(summary = "鑾峰緱鏂囩珷璇︽儏")
    @Parameters({
            @Parameter(name = "id", description = "鏂囩珷缂栧彿", example = "1024"),
            @Parameter(name = "title", description = "鏂囩珷鏍囬", example = "1024"),
    })
    public CommonResult<AppArticleRespVO> getArticle(@RequestParam(value = "id", required = false) Long id,
                                                     @RequestParam(value = "title", required = false) String title) {
        ArticleDO article = id != null ? articleService.getArticle(id)
                : articleService.getLastArticleByTitle(title);
        return success(BeanUtils.toBean(article, AppArticleRespVO.class));
    }

    @PutMapping("/add-browse-count")
    @Operation(summary = "澧炲姞鏂囩珷娴忚閲?)
    @Parameter(name = "id", description = "鏂囩珷缂栧彿", example = "1024")
    public CommonResult<Boolean> addBrowseCount(@RequestParam("id") Long id) {
        articleService.addArticleBrowseCount(id);
        return success(true);
    }

}