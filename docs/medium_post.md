# Where Does My Money Go?

### I built an Android app because apparently my money has a disappearing problem.

![Paisa Medium Cover](https://raw.githubusercontent.com/SuvarneshKM/paisa/refs/heads/main/docs/screenshots/medium_cover.png)

₹20 for tea.  
₹60 for snacks.  
₹150 for something I don't even remember buying.  

Nothing serious, right?

Then the month ends, I check my bank balance, and suddenly I'm a detective investigating my own spending.

*Where did all my money go?*

The funny part? I have absolutely no idea.

---

### Too many apps, one bank balance

In India, paying someone has become ridiculously easy.

Google Pay, PhonePe, Paytm, BHIM, banking apps... we've got plenty of options.

One day I pay using GPay. The next day it's PhonePe. Sometimes I just scan a QR code using whichever app opens first.

Every app has its own transaction history.

But when I want to know how much I've actually spent across everything, things get a little messy.

I don't want to open five different apps and mentally calculate where my money went.

I just want one place to understand my spending.

And no, switching to just one UPI app isn't the solution I'm looking for.

---

### It started with a friend

My friend [Jobin](https://github.com/JobinBiju) built an app called [Finlogue](https://github.com/JobinBiju/Finlogue).

He even wrote about it in his article, [I Built an App to Stop Lying to Myself About Money](https://jobinbiju.substack.com/p/i-built-an-app-to-stop-lying-to-myself).

Reading his story made me think:

*Wait, why don't I build something like this for Android?*

And that's how Paisa started.

---

### Meet Paisa

I wanted something simple.

An app that helps me understand where my money goes without making me manually enter every ₹20 tea purchase.

Because if tracking expenses feels like homework, I'm probably not going to do it.

So I started building Paisa.

The idea is pretty straightforward:

- **Track expenses across different payment apps in one place.**
- **Detect supported bank transaction SMS messages**, with permission.
- **Let me categorize payments quickly through notifications.**
- **Keep my financial data on my phone.**

Paisa doesn't need to connect to my UPI apps or log in to my bank. It works with the transaction information available through supported bank messages, along with anything I add manually.

No complicated setup. No connecting my bank account to some random service.

Just me, my money, and hopefully fewer financial mysteries.

![Paisa App Screenshot](https://raw.githubusercontent.com/SuvarneshKM/paisa/refs/heads/main/docs/screenshots/01_home_screen.png)

---

### Building it was... interesting

At first, I thought:

*It's just an expense tracker. How hard can it be?*

Turns out, money is surprisingly good at creating bugs.

Bank messages aren't always consistent. Transfers aren't necessarily expenses. And sometimes the app confidently tells me my income is ₹0 while showing money I just received.

Very reassuring.

But that's part of building something.

You make it work. You find something broken. You fix it. And then something else breaks.

At least the app looks nice while doing it.

![Paisa Bank Cards & Notifications](https://raw.githubusercontent.com/SuvarneshKM/paisa/refs/heads/main/docs/screenshots/02_accounts_screen.png)

---

### What's next?

Paisa is still a work in progress.

I'm planning to use it on my own phone, see what breaks, and keep improving it.

I'm also making it open source, because maybe someone else wants to understand where their money goes too.

Or maybe someone just wants to fix my bugs. Both are welcome.

Check out Paisa on GitHub:  
👉 [github.com/SuvarneshKM/paisa](https://github.com/SuvarneshKM/paisa)

And thanks to Jobin for the inspiration.

Anyway, I built an app to track my money.  
Now I just need to stop spending it.
