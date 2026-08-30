export interface RequirementMatch {
  requirement: string;
  status: "met" | "partial" | "missing";
  evidence: string;
}

export interface AnalyzeResponse {
  candidateName: string;
  roleTitle: string;
  tier: string;
  summary: string;
  requirements: RequirementMatch[];
  matchedSkills: string[];
  certificateBase64: string;
  verificationId: string;
}

export interface ModelOption {
  id: string;
  displayName: string;
  description: string;
}
