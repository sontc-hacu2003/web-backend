package backend.web.core.model.entity.admin;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "cms_function")
public class CmsFunction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "func_id")
    private Long funcId;

    @Column(name = "func_code", nullable = false, unique = true, length = 50)
    private String funcCode;

    @Column(name = "func_name", nullable = false, length = 200)
    private String funcName;

    @Column(name = "func_order")
    private Long funcOrder;

    @Column(name = "func_display", length = 1)
    private String funcDisplay;

    @Column(name = "func_level")
    private Long funcLevel;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "func_url", length = 200)
    private String funcUrl;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "func_icon", length = 50)
    private String funcIcon;
}
