export interface Module {
  moduleId: number;
  name: string;
}

export interface University {
  universityId: number;
  name: string;
  modules: Module[];
  isMember: boolean; // <-- ADD THIS
}