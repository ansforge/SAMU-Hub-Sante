const { validatePayload } = require('./modelesRouter');

const generateValidInput = () => ({
  password: 'mockPassword',
  fileName: 'mockFilePath.txt',
  content: 'mockFileContent',
  branchConfig: {
    isNewBranch: false,
    branch: 'auto-json-creator/mock-branch-name',
  },
});

const generateValidInputWithNewBranch = () => {
  const result = generateValidInput();
  result.branchConfig = {
    isNewBranch: true,
    branch: 'mock-branch-name',
    newBranch: 'auto-json-creator/mock-branch-name',
  };
  return result;
};

describe('modelesRouter - validatePayload', () => {
  it('validate payload with existing branch', () => {
    const input = generateValidInput();

    expect(() => validatePayload(input)).not.toThrow();
  });

  it('throws when isNewBranch is false and branch name is invalid', () => {
    const input = generateValidInput();
    input.branchConfig.branch = 'invalid-branch-name';

    expect(() => validatePayload(input)).toThrow();
  });

  it('validate payload with new branch', () => {
    const input = generateValidInputWithNewBranch();

    expect(() => validatePayload(input)).not.toThrow();
  });

  it('throws when isNewBranch is true and no newBranchName is porvided', () => {
    const input = generateValidInputWithNewBranch();
    delete input.branchConfig.newBranch;

    expect(() => validatePayload(input)).toThrow();
  });

  it('throws when isNewBranch is true and new branch name is invalid', () => {
    const input = generateValidInputWithNewBranch();
    input.branchConfig.newBranch = 'invalid-branch-name';

    expect(() => validatePayload(input)).toThrow();
  });
});
