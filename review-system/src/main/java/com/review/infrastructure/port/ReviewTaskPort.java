package com.review.infrastructure.port;

import java.util.Optional;

public interface ReviewTaskPort {

    Optional<Object> findById(Long id);

    Object save(Object task);
}
