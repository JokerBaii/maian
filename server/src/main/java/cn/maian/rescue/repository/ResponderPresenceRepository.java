package cn.maian.rescue.repository;

import cn.maian.rescue.domain.ResponderPresence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ResponderPresenceRepository extends JpaRepository<ResponderPresence, UUID> {
}
