package client.mybatis;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserMapper {

    @Select("SELECT id, name FROM users WHERE id = #{id}")
    User selectById(@Param("id") long id);
}

