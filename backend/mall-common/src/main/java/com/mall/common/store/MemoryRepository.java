package com.mall.common.store;

import com.mall.common.model.Entity;
import com.mall.common.util.TimeUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * 基于内存（ConcurrentHashMap）的通用仓储实现。
 * <p>当前阶段按需求「数据库先不用」实现；MyBatis/MySQL 接入点：
 * 将 insert/update/delete/findById/findAll 等方法替换为 Mapper 调用即可，
 * 业务层对仓储的调用方式保持不变。</p>
 *
 * @param <E> 实体类型（继承 {@link Entity}）
 */
public class MemoryRepository<E extends Entity> {

    private final String name;
    private final ConcurrentHashMap<Long, E> store = new ConcurrentHashMap<>();
    private final AtomicLong idSeq = new AtomicLong(0);

    public MemoryRepository(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /** 生成下一个主键 */
    public Long nextId() {
        return idSeq.incrementAndGet();
    }

    /** 插入并回填 id / createTime / updateTime */
    public E insert(E e) {
        Long id = e.getId();
        if (id == null) {
            id = nextId();
        }
        e.setId(id);
        String now = TimeUtils.now();
        e.setCreateTime(now);
        e.setUpdateTime(now);
        store.put(id, e);
        return e;
    }

    /** 批量插入 */
    public void insertAll(List<E> list) {
        for (E e : list) {
            insert(e);
        }
    }

    /** 更新（含更新时间回填）；不存在则插入 */
    public E update(E e) {
        if (e.getId() == null) {
            return insert(e);
        }
        e.setUpdateTime(TimeUtils.now());
        store.put(e.getId(), e);
        return e;
    }

    public void delete(Long id) {
        store.remove(id);
    }

    public Optional<E> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public boolean exists(Long id) {
        return store.containsKey(id);
    }

    /** 全部数据（按 id 升序） */
    public List<E> findAll() {
        List<E> list = new ArrayList<>(store.values());
        list.sort(Comparator.comparingLong(a -> a.getId() == null ? 0 : a.getId()));
        return list;
    }

    /** 条件查询 */
    public List<E> findBy(Predicate<E> predicate) {
        List<E> list = new ArrayList<>();
        for (E e : store.values()) {
            if (predicate.test(e)) {
                list.add(e);
            }
        }
        list.sort(Comparator.comparingLong(a -> a.getId() == null ? 0 : a.getId()));
        return list;
    }

    public Optional<E> findOne(Predicate<E> predicate) {
        return store.values().stream().filter(predicate).findFirst();
    }

    public long count() {
        return store.size();
    }

    public long count(Predicate<E> predicate) {
        return store.values().stream().filter(predicate).count();
    }

    /** 分页查询：current 从 1 开始。返回 records 列表。 */
    public List<E> page(int current, int size, Predicate<E> predicate) {
        List<E> list = predicate == null ? findAll() : findBy(predicate);
        if (size <= 0) {
            return list;
        }
        int from = Math.min((current - 1) * size, list.size());
        int to = Math.min(from + size, list.size());
        return new ArrayList<>(list.subList(from, Math.max(from, to)));
    }

    /** 清空（测试/重置用） */
    public void clear() {
        store.clear();
    }
}