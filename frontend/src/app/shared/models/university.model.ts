export interface Module {
  moduleId: number;
  name: string;
}
export interface ModuleDetail extends Module {
  university: {
    universityId: number;
    name: string;
    isMember: boolean; // ✅ ADD THIS LINE

  };
}

export interface University {
  universityId: number;
  name: string;
  modules: Module[];
  isMember: boolean;
  memberCount: number; // <-- ADD THIS PROPERTY
}
