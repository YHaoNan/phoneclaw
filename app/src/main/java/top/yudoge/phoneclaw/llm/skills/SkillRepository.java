package top.yudoge.phoneclaw.llm.skills;

import java.util.List;

public interface SkillRepository {

    List<Skill> loadSkills();

    Skill getSkill(String name);

    void addSkill(Skill skill);

    void updateSkill(Skill skill);

    void deleteSkill(Skill skill);

    void deleteSkillByName(String name);

    boolean skillExists(String name);

}
