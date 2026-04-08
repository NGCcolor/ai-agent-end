package cn.bugstack.xfg.dev.tech;


import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;



@Configurable
@SpringBootApplication
public class Application {

    /**
     * 核心修改：排除 OpenAI 自动配置类，阻止其初始化
     * 备注：@Configurable 注解在这里是多余的，可移除（SpringBootApplication 已包含核心配置能力）
     */

    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }

}
