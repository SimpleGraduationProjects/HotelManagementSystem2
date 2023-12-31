package com.hotel.system.controller.admin;

import cn.hutool.http.HtmlUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.system.controller.common.BaseController;
import com.hotel.system.dto.JsonResult;
import com.hotel.system.dto.QueryCondition;
import com.hotel.system.entity.Category;
import com.hotel.system.entity.Post;
import com.hotel.system.enums.PostStatusEnum;
import com.hotel.system.enums.ResultCodeEnum;
import com.hotel.system.exception.MyBusinessException;
import com.hotel.system.service.CategoryService;
import com.hotel.system.service.PostService;
import com.hotel.system.util.PageUtil;
import com.hotel.system.util.RegexUtil;
import com.hotel.system.util.SensUtils;
import com.hotel.system.vo.SearchVo;
import com.hotel.system.util.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * <pre>
 *     后台客房管理控制器
 * </pre>
 *
 * @author : wpx
 * @date : 2022/04/10
 */
@Controller
@RequestMapping(value = "/admin/post")
public class PostController extends BaseController {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(PostController.class);
    @Autowired
    private PostService postService;

    @Autowired
    private CategoryService categoryService;

    public static final String TITLE = "title";

    public static final String CONTENT = "content";


    /**
     * 处理后台获取客房列表的请求
     *
     * @param model model
     * @return 模板路径admin/admin_post
     */
    @GetMapping
    public String posts(Model model,
                        @RequestParam(value = "status", defaultValue = "0") Integer status,
                        @RequestParam(value = "keywords", defaultValue = "") String keywords,
                        @RequestParam(value = "searchType", defaultValue = "") String searchType,
                        @RequestParam(value = "postSource", defaultValue = "-1") Integer postSource,
                        @RequestParam(value = "page", defaultValue = "1") Integer pageNumber,
                        @RequestParam(value = "size", defaultValue = "10") Integer pageSize,
                        @RequestParam(value = "sort", defaultValue = "createTime") String sort,
                        @RequestParam(value = "order", defaultValue = "desc") String order,
                        @ModelAttribute SearchVo searchVo) {

        Post condition = new Post();
        if (!StringUtils.isBlank(keywords)) {
            if (TITLE.equals(searchType)) {
                condition.setPostTitle(keywords);
            } else {
                condition.setPostContent(keywords);
            }
        }
        condition.setPostStatus(status);

        Page page = PageUtil.initMpPage(pageNumber, pageSize, sort, order);
        Page<Post> posts = postService.findAll(
                page,
                new QueryCondition<>(condition, searchVo));

        List<Post> postList = posts.getRecords();
        for(Post post : postList) {
            Category category = categoryService.get(post.getCateId());
            post.setCategory(category == null ? new Category() : category);
        }
        //封装分类和标签
        model.addAttribute("posts", postList);
        model.addAttribute("pageInfo", PageUtil.convertPageVo(page));
        model.addAttribute("status", status);
        model.addAttribute("keywords", keywords);
        model.addAttribute("searchType", searchType);
        model.addAttribute("postSource", postSource);
        model.addAttribute("order", order);
        model.addAttribute("sort", sort);
        return "admin/admin_post";
    }


    /**
     * 处理跳转到新建客房页面
     *
     * @return 模板路径admin/admin_editor
     */
    @GetMapping(value = "/new")
    public String newPost(Model model) {
        //所有分类
        List<Category> allCategories = categoryService.findAll();
        model.addAttribute("categories", allCategories);
        return "admin/admin_post_new";
    }



    /**
     * 添加/更新客房
     *
     * @param post    Post实体
     */
    @PostMapping(value = "/save")
    @ResponseBody
    public JsonResult pushPost(@ModelAttribute Post post) {
        // 1、提取摘要
        int postSummary = 100;
        //客房摘要
        String summaryText = HtmlUtil.cleanHtmlTag(post.getPostContent());
        if (summaryText.length() > postSummary) {
            String summary = summaryText.substring(0, postSummary);
            post.setPostSummary(summary);
        } else {
            post.setPostSummary(summaryText);
        }

        // 2.处理imgUrl
        String postEditor = post.getPostEditor();
        if(StringUtils.isNotEmpty(postEditor)) {
            List<String> urlList = RegexUtil.getImgSrc(postEditor);
            String imgUrl = SensUtils.listToStr(urlList);
            post.setImgUrl(imgUrl);
        }

        // 2.添加/更新入库
        postService.insertOrUpdate(post);
        return JsonResult.success("发布成功");
    }


    /**
     * 处理移至回收站的请求
     *
     * @param postId 客房编号
     * @return 重定向到/admin/post
     */
    @PostMapping(value = "/throw")
    @ResponseBody
    public JsonResult moveToTrash(@RequestParam("id") Long postId) {
        Post post = postService.get(postId);
        if (post == null) {
            throw new MyBusinessException("客房不存在");
        }
        post.setPostStatus(PostStatusEnum.RECYCLE.getCode());
        postService.update(post);
        return JsonResult.success("操作成功");

    }

    /**
     * 处理客房为发布的状态
     *
     * @param postId 客房编号
     * @return 重定向到/admin/post
     */
    @PostMapping(value = "/revert")
    @ResponseBody
    public JsonResult moveToPublish(@RequestParam("id") Long postId) {
        Post post = postService.get(postId);
        if (post == null) {
            throw new MyBusinessException("客房不存在");
        }
        post.setPostStatus(PostStatusEnum.PUBLISHED.getCode());
        postService.update(post);
        return JsonResult.success("操作成功");
    }



    /**
     * 处理删除客房的请求
     *
     * @param postId 客房编号
     * @return 重定向到/admin/post
     */
    @PostMapping(value = "/delete")
    @ResponseBody
    public JsonResult removePost(@RequestParam("id") Long postId) {
        Post post = postService.get(postId);
        if (post == null) {
            throw new MyBusinessException("客房不存在");
        }
        postService.delete(postId);
        return JsonResult.success("删除成功");
    }

    /**
     * 批量删除
     *
     * @param ids 客房ID列表
     * @return 重定向到/admin/post
     */
    @PostMapping(value = "/batchDelete")
    @ResponseBody
    public JsonResult batchDelete(@RequestParam("ids") List<Long> ids) {
        //批量操作
        //1、防止恶意操作
        if (ids == null || ids.size() == 0 || ids.size() >= 100) {
            return new JsonResult(ResultCodeEnum.FAIL.getCode(), "参数不合法!");
        }
        //2、检查用户权限
        //客房作者才可以删除
        List<Post> postList = postService.findByBatchIds(ids);
        //3、如果当前状态为回收站，则删除；否则，移到回收站
        for (Post post : postList) {
            if (Objects.equals(post.getPostStatus(), PostStatusEnum.RECYCLE.getCode())) {
                postService.delete(post.getId());
            } else {
                post.setPostStatus(PostStatusEnum.RECYCLE.getCode());
                postService.update(post);
            }
        }
        return JsonResult.success("删除成功");
    }


    /**
     * 跳转到编辑客房页面
     *
     * @param postId 客房编号
     * @param model  model
     * @return 模板路径admin/admin_editor
     */
    @GetMapping(value = "/edit")
    public String editPost(@RequestParam("id") Long postId, Model model) {
        Post post = postService.get(postId);
        if (post == null) {
            throw new MyBusinessException("客房不存在");
        }

        //当前客房分类
        Category category = categoryService.get(post.getCateId());
        post.setCategory(category);
        model.addAttribute("post", post);


        //所有分类
        List<Category> allCategories = categoryService.findAll();
        model.addAttribute("categories", allCategories);
        return "admin/admin_post_edit";
    }




}
