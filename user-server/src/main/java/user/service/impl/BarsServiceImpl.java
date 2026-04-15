package user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import user.domain.po.Bars;
import user.mapper.BarsMapper;
import user.service.IBarsService;


@Slf4j
@Service
public class BarsServiceImpl extends ServiceImpl<BarsMapper, Bars>
        implements IBarsService {
}
