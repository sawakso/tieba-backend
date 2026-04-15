package user.domain.dao;

public class SmsDAO {
    // 模拟保存短信发送记录（这里可以连接数据库或其他数据源）
    public void saveSmsLog(String phone, String messageContent) {
        // 这里你可以将短信日志保存到数据库
        System.out.println("短信日志：发送到 " + phone + "，内容：" + messageContent);
    }
}
