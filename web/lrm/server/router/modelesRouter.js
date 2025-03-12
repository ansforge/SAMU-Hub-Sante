const express = require('express');
const config = require('../config');
const { getModelesBranchNames, createNewBranch, commitModelesChangesToExistingBranch } = require('../service/modeles');

const VALIDATION_ERROR_MESSAGE = 'Missing mandatory attribute in payload';

const AUTHORIZED_BRANCH_PATTERN = /auto-json-creator\/.*/;
const AUTHORIZED_BRANCH_ERROR_MESSAGE = 'Invalid branch name';

const getModelesBranchesHandler = async (_, res) => {
  const branchNames = await getModelesBranchNames();
  res.status(200).json(branchNames);
};

const validatePayload = (body) => {
  if (!body.fileName) {
    throw new Error(`${VALIDATION_ERROR_MESSAGE}: fileName (name of the file to update)`);
  }
  if (!body.content) {
    throw new Error(`${VALIDATION_ERROR_MESSAGE}: content (content of the file to update)`);
  }
  if (!body.branchConfig) {
    throw new Error(`${VALIDATION_ERROR_MESSAGE}: branchConfig`);
  }
  if (!body.branchConfig.branch) {
    throw new Error(`${VALIDATION_ERROR_MESSAGE}: branchConfig.branch (branch to update)`);
  }
  // Check the branchConfig.branch name matching the authorized pattern
  // only if therer is no new branch in branchConfig (to avoid direct
  // commit on unauthorized branch)
  if (
    !body.branchConfig.isNewBranch
    && !body.branchConfig.branch.match(AUTHORIZED_BRANCH_PATTERN)
  ) {
    throw new Error(
      `${AUTHORIZED_BRANCH_ERROR_MESSAGE}: branchConfig.branch must match "${AUTHORIZED_BRANCH_PATTERN}"`,
    );
  }
  if (body.branchConfig.isNewBranch === undefined) {
    throw new Error(`${VALIDATION_ERROR_MESSAGE}: branchConfig.isNewBranch (branch to update)`);
  }
  if (body.branchConfig.isNewBranch) {
    if (!body.branchConfig.newBranch) {
      throw new Error(
        `${VALIDATION_ERROR_MESSAGE}: branchConfig.newBranch (required because branchConfig.isNewBranch is set to true)`,
      );
    }
    if (!body.branchConfig.newBranch.match(AUTHORIZED_BRANCH_PATTERN)) {
      throw new Error(
        `${AUTHORIZED_BRANCH_ERROR_MESSAGE}: branchConfig.newBranch must match "${AUTHORIZED_BRANCH_PATTERN}"`,
      );
    }
  }
};

const commitModelesChanges = async (req, res) => {
  const {
    fileName,
    content,
    branchConfig,
    password,
  } = req.body;

  if (password !== config.ADMIN_PASSWORD) {
    res.status(401).json({ message: 'Unauthorized' });
    return;
  }

  try {
    validatePayload(req.body);
  } catch (err) {
    res.status(403).json({ message: err.message });
    return;
  }

  try {
    if (branchConfig.isNewBranch) {
      await createNewBranch({
        baseBranch: branchConfig.baseBranch,
        newBranch: branchConfig.branch,
      });
    }

    const result = await commitModelesChangesToExistingBranch({
      branch: branchConfig.branch,
      fileName,
      content,
    });

    res.status(200).json({ message: 'Commit created', data: result });
  } catch (err) {
    res
      .status(500)
      .json({
        message: `An unexpected error happend: ${
          err.message || 'Internal Server Error'
        }`,
      });
  }
};

const ModelesRouter = express.Router();

ModelesRouter.get('/branches', getModelesBranchesHandler);

ModelesRouter.post('/', commitModelesChanges);

module.exports = ModelesRouter;
