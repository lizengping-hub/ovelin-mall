package com.ovelin.mall.common.web.starter.jackson;

import java.util.List;

record IdBody(Long id, List<Long> ids, Integer count) {}
