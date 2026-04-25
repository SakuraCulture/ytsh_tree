package cn.iocoder.yudao.module.business.enums.tag;

import java.util.Set;

public interface TagConstants {

    Long ROOT_PARENT_ID = 0L;
    Integer LEVEL_L1 = 1;
    Integer LEVEL_L2 = 2;
    Integer LEVEL_L3 = 3;
    Integer STATUS_DISABLED = 0;
    Integer STATUS_ENABLED = 1;

    Set<String> DOMAIN_TYPES = Set.of("PRODUCT", "STORE", "MEMBER");
    Set<String> TAG_METHODS = Set.of("MANUAL", "RULE", "ALGORITHM", "INHERIT");

}
