package user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import user.domain.dto.Result;
import user.domain.po.Bars;
import user.service.IBarsService;

import javax.annotation.Resource;
import java.util.List;

//吧模块相关
@Api(tags = "吧模块接口")
@RestController
@RequestMapping("/bars")
public class BarsController {

    @Resource
    private IBarsService barsService;
    // 新增：获取单个吧详情
    @ApiOperation("根据ID查询吧详情")
    @GetMapping("/{id}")
    public Result getBarById(@PathVariable Long id) {
        Bars bar = barsService.getById(id);
        if (bar != null) {
            return Result.ok(bar);
        } else {
            return Result.fail("吧不存在");
        }
    }
    //查全部
    @ApiOperation("查询全部吧")
    @GetMapping( "/list")
    public Result list() {
        return Result.ok(barsService.list());
    }
    //创建吧
    @ApiOperation("创建吧")
    @PostMapping("/add")
    public Result add(@RequestBody Bars bar) {
        barsService.save(bar);
        return Result.ok();
    }
    //修改吧
    @ApiOperation("修改吧")
    @PutMapping("/update")
    public Result update(@RequestBody Bars bar) {
        barsService.updateById(bar);
        return Result.ok();
    }
    //删除吧
    @ApiOperation("删除吧")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        barsService.removeById(id);
        return Result.ok();
    }
    @ApiOperation("加入吧")
    //加入吧，成员+1
    @PostMapping("/{id}/join")
    public Result join(@PathVariable Long id) {
        barsService.lambdaUpdate()
                .eq(Bars::getId, id)
                .setSql("member_count = member_count + 1")
                .update();
        return Result.ok();
    }
    //退出吧，成员减一
    @ApiOperation("退出吧")
    @PostMapping("/{id}/quit")
    public Result quit(@PathVariable Long id) {
        barsService.lambdaUpdate()
                .eq(Bars::getId, id)
                .setSql("member_count = member_count - 1")
                .update();
        return Result.ok();
    }

    //分页查询吧
    @ApiOperation("分页查询吧")
    @GetMapping("/page")
    public Result page(
            @RequestParam Integer page,
            @RequestParam Integer size,
            @RequestParam(required = false) String name
    ) {
        Page<Bars> p = new Page<>(page, size);

        return Result.ok(
                barsService.lambdaQuery()
                        .like(name != null, Bars::getName, name)
                        .eq(Bars::getStatus, 1)
                        .orderByDesc(Bars::getMemberCount)
                        .page(p)
        );
    }

}