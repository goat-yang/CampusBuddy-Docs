A 的用户、标签、刷新令牌和幂等表在 `migration/V1__a_user.sql`，由 Flyway 执行。B 的活动表用 `V2__` 或更后的版本，不要再占用 V1。不要把生产 dump 提交进 Git。
