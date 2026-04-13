const express = require("express");
const { handleAuthRequest, } = require("../Controller/authController");

const router = express.Router();


const { extractToken } = require("../Middleware/extractUid");

router.get("/", extractToken, handleAuthRequest);

module.exports = router;