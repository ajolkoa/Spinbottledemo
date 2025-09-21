
const express = require('express');
const Stripe = require('stripe');
require('dotenv').config();

const app = express();
app.use(express.json());

const stripe = Stripe(process.env.STRIPE_SECRET_KEY);

app.post('/create-payment-intent', async (req, res) => {
  try {
    const paymentIntent = await stripe.paymentIntents.create({
      amount: 1000, // 10.00 TRY/USD vs
      currency: 'usd',
      automatic_payment_methods: { enabled: true },
    });
    res.send({ clientSecret: paymentIntent.client_secret });
  } catch (err) {
    res.status(400).send({ error: err.message });
  }
});

app.listen(4242, () => console.log("Server running on port 4242"));
