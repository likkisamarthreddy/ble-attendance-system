const crypto = require('crypto');

function generateToken(sessionSecret, timeSlot) {
  const mac = crypto.createHmac("sha256", sessionSecret);
  const slotBytes = Buffer.alloc(8);
  slotBytes.writeBigInt64BE(BigInt(timeSlot));
  const fullHash = mac.update(slotBytes).digest();
  return fullHash.subarray(0, 8).toString("hex");
}

const secret = "3c654e060acd181855d521ff731b64d02fb3c1a43b3bb5eec22e2d41f91eada4";
const targetToken = "662660ae";
const centerSlot = 253321578;
const range = 50000; // Large range

console.log("Searching for " + targetToken + " around slot " + centerSlot);

for (let offset = -range; offset <= range; offset++) {
    const slot = centerSlot + offset;
    const token = generateToken(secret, slot);
    if (token.startsWith(targetToken)) {
        console.log(`FOUND ${targetToken} at slot ${slot} (offset ${offset})`);
    }
}
console.log("Search complete");

