package com.brodong.enjoyable_animal_husbanding.accessor;

/**
 * 性别访问器接口，为 Mixin 注入的 {@link Animal} 实体提供性别读写能力。
 * <p>
 * 任何实现了此接口的实体都可以通过 {@link #getGender()} / {@link #setGender(Gender)}
 * 方法查询或修改其性别，供繁殖逻辑和外部交互使用。
 */
public interface GenderAccessor {
    Gender getGender();
    void setGender(Gender gender);
}
