## Config Table integration with Spring Boot

### Ⅰ.简介
在我们游戏日常开发中经常需要将策划的 *Excel* 配置表加载到我们游戏的业务中进行处理，通常的做法是直接读取 *Excel* 表或者通过一些工具将其转换为其它格式然后加入到我们
游戏中，本组件的主要功能就是将策划的配置表转换为我们业务中的所需要访问的接口。

### Ⅱ.背景和适用项目
- 适用于Web项目、游戏项目等
- 简单易用，简单配置，优雅的代码

### Ⅲ.Maven依赖
- JDK版本要求JDK17+
```xml
<dependency>
    <groupId>com.think</groupId>
    <artifactId>table-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```
- 定义配置表,这里以 *CSV* 为例
```csv
id|name|kind|type
1001|每日签到1次|1|1
1002|好友数量达到5个|1|2
1003|挑战胜利1次|1|3
```
- 在我们日常开发过程中为了防止业务代码中篡改其配置表数据推荐使用record类或者构造函数不可变字段来使用;
```java
@CfgTable("任务表")
public record CfgTask(
        int id,
        String name,
        int kind,
        int type) {
}
```
这里我们使用到了 @CfgTable 注解，注解中的内容是我们读取的配置表文件名，这个文件名可以是 *csv*、*xlsx*、*json*，字段就是我们对应配置表中的内容;
- 定义对应的 Repository 接口
```java
@TableRepository
public interface CfgTaskRepository extends CfgRepository<CfgTask, Integer> {
}

```
这里我们继承了 CfgRepository 接口类，其中泛型的第一个参数表示配置表的类，第二个泛型参数表示配置表唯一ID的类型，一般我们采用数字类型表示，同时在该类的头部定义了 @TableRepository 注解，标记了该注解的接口类才会被接管处理;
- 使用范例
```java
public interface CfgRepository<T, Serializable> {
    T findById(Serializable id);

    Optional<T> findById(Predicate<T> predicate);

    List<T> findAll(Predicate<T> predicate);

    List<T> findAll();

    long count(Predicate<T> predicate);

    boolean exists(Serializable id);

    boolean exists(Predicate<T> predicate);
}
```
目前提供了这几个接口，分别是根据主键ID来查找一个对象，查找所有，统计个数，判断是否存在等;
```java
@RestController
@RequestMapping("/api/table")
public class TableController {
    private final CfgTaskRepository cfgTaskRepository;

    public TableController(CfgTaskRepository cfgTaskRepository) {
        this.cfgTaskRepository = cfgTaskRepository;
    }

    @GetMapping("/task")
    public List<CfgTask> tasks() {
        return cfgTaskRepository.findAll();
    }

    @GetMapping("/task/{taskId}")
    public CfgTask task(@PathVariable int taskId) {
        return cfgTaskRepository.findById(taskId);
    }
}
```
### Ⅳ.使用案例
[table-spring-boot-starter-example](https://github.com/veione/table-spring-boot-starter-example)
