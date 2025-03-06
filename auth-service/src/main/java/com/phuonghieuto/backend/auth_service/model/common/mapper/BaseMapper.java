package com.phuonghieuto.backend.auth_service.model.common.mapper;

import java.util.Collection;
import java.util.List;


public interface BaseMapper<S, T> {

    T map(S source);

    List<T> map(Collection<S> sources);

}